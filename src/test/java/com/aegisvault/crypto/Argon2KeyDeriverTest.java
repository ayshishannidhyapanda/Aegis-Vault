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

class Argon2KeyDeriverTest {

    @Test
    void deriveKeyProduces32Bytes() {
        char[] password = "testPassword123!".toCharArray();
        byte[] salt = SecureRandomProvider.generateSalt();

        byte[] key = Argon2KeyDeriver.deriveKey(password, salt);

        assertEquals(SecureRandomProvider.KEY_SIZE_BYTES, key.length);
    }

    @Test
    void samePasswordAndSaltProduceSameKey() {
        char[] password1 = "samePassword".toCharArray();
        char[] password2 = "samePassword".toCharArray();
        byte[] salt = SecureRandomProvider.generateSalt();

        byte[] key1 = Argon2KeyDeriver.deriveKey(password1, salt);
        byte[] key2 = Argon2KeyDeriver.deriveKey(password2, salt);

        assertArrayEquals(key1, key2);
    }

    @Test
    void differentPasswordsProduceDifferentKeys() {
        char[] password1 = "password1".toCharArray();
        char[] password2 = "password2".toCharArray();
        byte[] salt = SecureRandomProvider.generateSalt();

        byte[] key1 = Argon2KeyDeriver.deriveKey(password1, salt);
        byte[] key2 = Argon2KeyDeriver.deriveKey(password2, salt);

        assertFalse(Arrays.equals(key1, key2));
    }

    @Test
    void differentSaltsProduceDifferentKeys() {
        char[] password = "samePassword".toCharArray();
        byte[] salt1 = SecureRandomProvider.generateSalt();
        byte[] salt2 = SecureRandomProvider.generateSalt();

        byte[] key1 = Argon2KeyDeriver.deriveKey(password.clone(), salt1);
        byte[] key2 = Argon2KeyDeriver.deriveKey(password.clone(), salt2);

        assertFalse(Arrays.equals(key1, key2));
    }

    @Test
    void nullPasswordThrows() {
        byte[] salt = SecureRandomProvider.generateSalt();

        assertThrows(IllegalArgumentException.class, () -> Argon2KeyDeriver.deriveKey(null, salt));
    }

    @Test
    void emptyPasswordThrows() {
        byte[] salt = SecureRandomProvider.generateSalt();

        assertThrows(IllegalArgumentException.class, () -> Argon2KeyDeriver.deriveKey(new char[0], salt));
    }

    @Test
    void nullSaltThrows() {
        char[] password = "test".toCharArray();

        assertThrows(IllegalArgumentException.class, () -> Argon2KeyDeriver.deriveKey(password, null));
    }

    @Test
    void wrongSaltSizeThrows() {
        char[] password = "test".toCharArray();
        byte[] shortSalt = new byte[16];

        assertThrows(IllegalArgumentException.class, () -> Argon2KeyDeriver.deriveKey(password, shortSalt));
    }

    @Test
    void zeroBytesZerosArray() {
        byte[] data = {1, 2, 3, 4, 5};

        Argon2KeyDeriver.zeroBytes(data);

        for (byte b : data) {
            assertEquals(0, b);
        }
    }

    @Test
    void zeroCharsZerosArray() {
        char[] data = {'a', 'b', 'c'};

        Argon2KeyDeriver.zeroChars(data);

        for (char c : data) {
            assertEquals('\0', c);
        }
    }

    @Test
    void zeroBytesHandlesNull() {
        assertDoesNotThrow(() -> Argon2KeyDeriver.zeroBytes(null));
    }

    @Test
    void zeroCharsHandlesNull() {
        assertDoesNotThrow(() -> Argon2KeyDeriver.zeroChars(null));
    }

    @Test
    void toBytesConvertsUtf8() {
        char[] chars = "Test123".toCharArray();

        byte[] bytes = Argon2KeyDeriver.toBytes(chars);

        assertEquals(7, bytes.length);
        assertEquals('T', bytes[0]);
        assertEquals('e', bytes[1]);
        assertEquals('s', bytes[2]);
        assertEquals('t', bytes[3]);
    }

    @Test
    void toBytesHandlesUnicodeCharacters() {
        char[] chars = "Tëst€".toCharArray();

        byte[] bytes = Argon2KeyDeriver.toBytes(chars);

        assertNotNull(bytes);
        assertTrue(bytes.length > chars.length);
    }
}
