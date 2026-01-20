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

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SecureRandomProviderTest {

    @Test
    void generateSaltReturns32Bytes() {
        byte[] salt = SecureRandomProvider.generateSalt();

        assertEquals(32, salt.length);
    }

    @Test
    void generateIvReturns12Bytes() {
        byte[] iv = SecureRandomProvider.generateIv();

        assertEquals(12, iv.length);
    }

    @Test
    void generateKeyReturns32Bytes() {
        byte[] key = SecureRandomProvider.generateKey();

        assertEquals(32, key.length);
    }

    @Test
    void consecutiveSaltsAreDifferent() {
        byte[] salt1 = SecureRandomProvider.generateSalt();
        byte[] salt2 = SecureRandomProvider.generateSalt();

        assertFalse(Arrays.equals(salt1, salt2));
    }

    @Test
    void consecutiveIvsAreDifferent() {
        byte[] iv1 = SecureRandomProvider.generateIv();
        byte[] iv2 = SecureRandomProvider.generateIv();

        assertFalse(Arrays.equals(iv1, iv2));
    }

    @Test
    void consecutiveKeysAreDifferent() {
        byte[] key1 = SecureRandomProvider.generateKey();
        byte[] key2 = SecureRandomProvider.generateKey();

        assertFalse(Arrays.equals(key1, key2));
    }

    @Test
    void constantsHaveCorrectValues() {
        assertEquals(32, SecureRandomProvider.SALT_SIZE_BYTES);
        assertEquals(12, SecureRandomProvider.IV_SIZE_BYTES);
        assertEquals(32, SecureRandomProvider.KEY_SIZE_BYTES);
    }
}
