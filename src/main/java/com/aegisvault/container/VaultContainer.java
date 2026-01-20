/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 *
 * This software, known as "AegisVault-J", including its source code, documentation,
 * design, and associated materials, is the intellectual property of the author.
 *
 * No part of this software may be copied, modified, distributed, or used in
 * derivative works without explicit written permission from the copyright holder,
 * except for academic evaluation purposes.
 *
 * This software is provided "as is", without warranty of any kind, express or
 * implied, including but not limited to the warranties of merchantability,
 * fitness for a particular purpose, and noninfringement.
 */
package com.aegisvault.container;

import com.aegisvault.crypto.AesGcmCipher;
import com.aegisvault.crypto.Argon2KeyDeriver;
import com.aegisvault.crypto.SecureRandomProvider;
import com.aegisvault.exception.AuthenticationException;
import com.aegisvault.exception.CryptoException;
import com.aegisvault.exception.VaultException;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VaultContainer implements Closeable {

    private static final int ENCRYPTED_VAULT_KEY_SIZE = SecureRandomProvider.IV_SIZE_BYTES + SecureRandomProvider.KEY_SIZE_BYTES + 16;
    private static final int METADATA_BLOCK_OFFSET = VaultHeader.HEADER_SIZE + ENCRYPTED_VAULT_KEY_SIZE;

    private final Path vaultPath;
    private boolean open;
    private VaultHeader header;
    private byte[] vaultKey;
    private Map<String, byte[]> fileData;
    private RandomAccessFile raf;
    private FileChannel channel;
    private FileLock lock;

    public VaultContainer(Path vaultPath) {
        this.vaultPath = vaultPath;
        this.open = false;
        this.fileData = new HashMap<>();
    }

    public void create(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }

        if (Files.exists(vaultPath)) {
            throw new VaultException("Vault file already exists: " + vaultPath);
        }

        byte[] masterKey = null;
        try {
            byte[] salt = SecureRandomProvider.generateSalt();
            byte[] headerIv = SecureRandomProvider.generateIv();
            this.header = new VaultHeader(salt, headerIv);

            masterKey = Argon2KeyDeriver.deriveKey(password, salt);

            this.vaultKey = SecureRandomProvider.generateKey();

            byte[] encryptedVaultKey = AesGcmCipher.encrypt(vaultKey, masterKey);

            byte[] emptyMetadata = serializeMetadata(new HashMap<>());
            byte[] encryptedMetadata = AesGcmCipher.encrypt(emptyMetadata, vaultKey);

            this.raf = new RandomAccessFile(vaultPath.toFile(), "rw");
            this.channel = raf.getChannel();
            this.lock = channel.tryLock();

            if (lock == null) {
                throw new VaultException("Cannot acquire lock on vault file");
            }

            channel.write(ByteBuffer.wrap(header.toBytes()));
            channel.write(ByteBuffer.wrap(encryptedVaultKey));

            ByteBuffer metadataLengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            metadataLengthBuffer.putInt(encryptedMetadata.length);
            metadataLengthBuffer.flip();
            channel.write(metadataLengthBuffer);
            channel.write(ByteBuffer.wrap(encryptedMetadata));

            channel.force(true);

            this.fileData = new HashMap<>();
            this.open = true;
        } catch (IOException e) {
            throw new VaultException("Failed to create vault file", e);
        } finally {
            Argon2KeyDeriver.zeroBytes(masterKey);
            Argon2KeyDeriver.zeroChars(password);
        }
    }

    public void open(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }

        if (!Files.exists(vaultPath)) {
            throw new VaultException("Vault file does not exist: " + vaultPath);
        }

        byte[] masterKey = null;
        boolean success = false;
        try {
            this.raf = new RandomAccessFile(vaultPath.toFile(), "rw");
            this.channel = raf.getChannel();
            this.lock = channel.tryLock();

            if (lock == null) {
                throw new VaultException("Cannot acquire lock on vault file - may be in use");
            }

            ByteBuffer headerBuffer = ByteBuffer.allocate(VaultHeader.HEADER_SIZE);
            readFully(channel, headerBuffer);
            headerBuffer.flip();
            this.header = VaultHeader.parse(headerBuffer.array());

            ByteBuffer encryptedVaultKeyBuffer = ByteBuffer.allocate(ENCRYPTED_VAULT_KEY_SIZE);
            readFully(channel, encryptedVaultKeyBuffer);
            encryptedVaultKeyBuffer.flip();
            byte[] encryptedVaultKey = new byte[ENCRYPTED_VAULT_KEY_SIZE];
            encryptedVaultKeyBuffer.get(encryptedVaultKey);

            masterKey = Argon2KeyDeriver.deriveKey(password, header.getSalt());

            try {
                this.vaultKey = AesGcmCipher.decrypt(encryptedVaultKey, masterKey);
            } catch (CryptoException e) {
                throw new AuthenticationException("Invalid password or corrupted vault");
            }

            ByteBuffer metadataLengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            readFully(channel, metadataLengthBuffer);
            metadataLengthBuffer.flip();
            int metadataLength = metadataLengthBuffer.getInt();

            if (metadataLength < 0 || metadataLength > 100 * 1024 * 1024) {
                throw new VaultException("Invalid metadata length: " + metadataLength);
            }

            ByteBuffer encryptedMetadataBuffer = ByteBuffer.allocate(metadataLength);
            readFully(channel, encryptedMetadataBuffer);
            encryptedMetadataBuffer.flip();
            byte[] encryptedMetadata = new byte[metadataLength];
            encryptedMetadataBuffer.get(encryptedMetadata);
            byte[] decryptedMetadata = AesGcmCipher.decrypt(encryptedMetadata, vaultKey);
            this.fileData = deserializeMetadata(decryptedMetadata);

            this.open = true;
            success = true;
        } catch (IOException e) {
            throw new VaultException("Failed to open vault file", e);
        } finally {
            Argon2KeyDeriver.zeroBytes(masterKey);
            Argon2KeyDeriver.zeroChars(password);
            if (!success) {
                closeResources();
            }
        }
    }

    private void closeResources() {
        try {
            if (lock != null) {
                lock.release();
                lock = null;
            }
        } catch (IOException ignored) {
        }
        try {
            if (channel != null) {
                channel.close();
                channel = null;
            }
        } catch (IOException ignored) {
        }
        try {
            if (raf != null) {
                raf.close();
                raf = null;
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isOpen() {
        return open;
    }

    public byte[] readFile(String fileId) {
        ensureOpen();
        byte[] encrypted = fileData.get(fileId);
        if (encrypted == null) {
            return null;
        }
        return AesGcmCipher.decrypt(encrypted, vaultKey);
    }

    public void writeFile(String fileId, byte[] content) {
        ensureOpen();
        if (fileId == null || fileId.isEmpty()) {
            throw new IllegalArgumentException("File ID must not be null or empty");
        }
        byte[] encrypted = AesGcmCipher.encrypt(content, vaultKey);
        fileData.put(fileId, encrypted);
        persistMetadata();
    }

    public void deleteFile(String fileId) {
        ensureOpen();
        if (fileData.remove(fileId) != null) {
            persistMetadata();
        }
    }

    public void changePassword(char[] currentPassword, char[] newPassword) {
        if (!open) {
            throw new IllegalStateException("Vault must be open to change password");
        }

        if (newPassword == null || newPassword.length == 0) {
            throw new IllegalArgumentException("New password must not be null or empty");
        }

        byte[] newMasterKey = null;
        try {
            byte[] newSalt = SecureRandomProvider.generateSalt();
            byte[] newHeaderIv = SecureRandomProvider.generateIv();

            newMasterKey = Argon2KeyDeriver.deriveKey(newPassword, newSalt);

            byte[] encryptedVaultKey = AesGcmCipher.encrypt(vaultKey, newMasterKey);

            this.header = new VaultHeader(newSalt, newHeaderIv);

            channel.position(0);
            channel.write(ByteBuffer.wrap(header.toBytes()));
            channel.write(ByteBuffer.wrap(encryptedVaultKey));
            channel.force(true);
        } catch (IOException e) {
            throw new VaultException("Failed to change password", e);
        } finally {
            Argon2KeyDeriver.zeroBytes(newMasterKey);
            Argon2KeyDeriver.zeroChars(currentPassword);
            Argon2KeyDeriver.zeroChars(newPassword);
        }
    }

    @Override
    public void close() {
        if (vaultKey != null) {
            Arrays.fill(vaultKey, (byte) 0);
            vaultKey = null;
        }

        closeResources();

        this.open = false;
        this.header = null;
        this.fileData = new HashMap<>();
    }

    private void ensureOpen() {
        if (!open) {
            throw new IllegalStateException("Vault is not open");
        }
    }

    public Path getVaultPath() {
        return vaultPath;
    }

    private void persistMetadata() {
        try {
            byte[] serialized = serializeMetadata(fileData);
            byte[] encrypted = AesGcmCipher.encrypt(serialized, vaultKey);

            channel.position(METADATA_BLOCK_OFFSET);

            ByteBuffer lengthBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            lengthBuffer.putInt(encrypted.length);
            lengthBuffer.flip();
            channel.write(lengthBuffer);
            channel.write(ByteBuffer.wrap(encrypted));

            long newFileSize = channel.position();
            channel.truncate(newFileSize);

            channel.force(true);
        } catch (IOException e) {
            throw new VaultException("Failed to persist metadata", e);
        }
    }

    private byte[] serializeMetadata(Map<String, byte[]> data) {
        int totalSize = 4;
        for (Map.Entry<String, byte[]> entry : data.entrySet()) {
            totalSize += 4 + entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            totalSize += 4 + entry.getValue().length;
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(data.size());

        for (Map.Entry<String, byte[]> entry : data.entrySet()) {
            byte[] keyBytes = entry.getKey().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            buffer.putInt(keyBytes.length);
            buffer.put(keyBytes);
            buffer.putInt(entry.getValue().length);
            buffer.put(entry.getValue());
        }

        return buffer.array();
    }

    private Map<String, byte[]> deserializeMetadata(byte[] data) {
        Map<String, byte[]> result = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

        int count = buffer.getInt();
        for (int i = 0; i < count; i++) {
            int keyLength = buffer.getInt();
            byte[] keyBytes = new byte[keyLength];
            buffer.get(keyBytes);
            String key = new String(keyBytes, java.nio.charset.StandardCharsets.UTF_8);

            int valueLength = buffer.getInt();
            byte[] value = new byte[valueLength];
            buffer.get(value);

            result.put(key, value);
        }

        return result;
    }

    private void readFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of file");
            }
        }
    }
}
