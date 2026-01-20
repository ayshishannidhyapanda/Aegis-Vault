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

import com.aegisvault.crypto.SecureRandomProvider;
import com.aegisvault.exception.VaultException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class VaultHeader {

    public static final byte[] MAGIC = "AEGISVLT".getBytes(StandardCharsets.US_ASCII);
    public static final int HEADER_SIZE = 64;
    public static final short CURRENT_VERSION = 1;

    private static final int MAGIC_OFFSET = 0;
    private static final int VERSION_OFFSET = 8;
    private static final int FLAGS_OFFSET = 10;
    private static final int SALT_OFFSET = 12;
    private static final int HEADER_IV_OFFSET = 44;
    private static final int RESERVED_OFFSET = 56;
    private static final int RESERVED_SIZE = 8;

    private short version;
    private short flags;
    private byte[] salt;
    private byte[] headerIv;

    public VaultHeader(byte[] salt, byte[] headerIv) {
        if (salt == null || salt.length != SecureRandomProvider.SALT_SIZE_BYTES) {
            throw new IllegalArgumentException("Salt must be " + SecureRandomProvider.SALT_SIZE_BYTES + " bytes");
        }
        if (headerIv == null || headerIv.length != SecureRandomProvider.IV_SIZE_BYTES) {
            throw new IllegalArgumentException("Header IV must be " + SecureRandomProvider.IV_SIZE_BYTES + " bytes");
        }
        this.version = CURRENT_VERSION;
        this.flags = 0;
        this.salt = salt.clone();
        this.headerIv = headerIv.clone();
    }

    private VaultHeader(short version, short flags, byte[] salt, byte[] headerIv) {
        this.version = version;
        this.flags = flags;
        this.salt = salt;
        this.headerIv = headerIv;
    }

    public static VaultHeader parse(byte[] data) {
        if (data == null || data.length < HEADER_SIZE) {
            throw new VaultException("Invalid header data: insufficient length");
        }

        byte[] magic = Arrays.copyOfRange(data, MAGIC_OFFSET, MAGIC_OFFSET + MAGIC.length);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new VaultException("Invalid vault file: magic bytes mismatch");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        short version = buffer.getShort(VERSION_OFFSET);
        if (version > CURRENT_VERSION) {
            throw new VaultException("Unsupported vault version: " + version);
        }

        short flags = buffer.getShort(FLAGS_OFFSET);
        byte[] salt = Arrays.copyOfRange(data, SALT_OFFSET, SALT_OFFSET + SecureRandomProvider.SALT_SIZE_BYTES);
        byte[] headerIv = Arrays.copyOfRange(data, HEADER_IV_OFFSET, HEADER_IV_OFFSET + SecureRandomProvider.IV_SIZE_BYTES);

        return new VaultHeader(version, flags, salt, headerIv);
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);

        buffer.put(MAGIC);
        buffer.putShort(version);
        buffer.putShort(flags);
        buffer.put(salt);
        buffer.put(headerIv);
        buffer.put(new byte[RESERVED_SIZE]);

        return buffer.array();
    }

    public short getVersion() {
        return version;
    }

    public short getFlags() {
        return flags;
    }

    public byte[] getSalt() {
        return salt.clone();
    }

    public byte[] getHeaderIv() {
        return headerIv.clone();
    }
}
