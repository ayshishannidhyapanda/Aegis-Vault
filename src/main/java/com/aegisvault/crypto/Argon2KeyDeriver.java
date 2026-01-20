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
package com.aegisvault.crypto;

import com.aegisvault.exception.CryptoException;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class Argon2KeyDeriver {

    private static final int MEMORY_COST_KB = 65536;
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;
    private static final int OUTPUT_LENGTH = SecureRandomProvider.KEY_SIZE_BYTES;

    private Argon2KeyDeriver() {
    }

    public static byte[] deriveKey(char[] password, byte[] salt) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        if (salt == null || salt.length != SecureRandomProvider.SALT_SIZE_BYTES) {
            throw new IllegalArgumentException("Salt must be " + SecureRandomProvider.SALT_SIZE_BYTES + " bytes");
        }

        byte[] passwordBytes = null;
        try {
            passwordBytes = toBytes(password);

            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withMemoryAsKB(MEMORY_COST_KB)
                .withIterations(ITERATIONS)
                .withParallelism(PARALLELISM)
                .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);

            byte[] derivedKey = new byte[OUTPUT_LENGTH];
            generator.generateBytes(passwordBytes, derivedKey);

            return derivedKey;
        } catch (Exception e) {
            throw new CryptoException("Key derivation failed", e);
        } finally {
            zeroBytes(passwordBytes);
        }
    }

    static byte[] toBytes(char[] chars) {
        if (chars == null) {
            return new byte[0];
        }
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }

    public static void zeroBytes(byte[] data) {
        if (data != null) {
            Arrays.fill(data, (byte) 0);
        }
    }

    public static void zeroChars(char[] data) {
        if (data != null) {
            Arrays.fill(data, '\0');
        }
    }
}
