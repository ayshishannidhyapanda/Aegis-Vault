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

import java.security.SecureRandom;

public final class SecureRandomProvider {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static final int SALT_SIZE_BYTES = 32;

    public static final int IV_SIZE_BYTES = 12;

    public static final int KEY_SIZE_BYTES = 32;

    private SecureRandomProvider() {
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] generateIv() {
        byte[] iv = new byte[IV_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    public static byte[] generateKey() {
        byte[] key = new byte[KEY_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(key);
        return key;
    }

    public static byte[] generateBytes(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
}
