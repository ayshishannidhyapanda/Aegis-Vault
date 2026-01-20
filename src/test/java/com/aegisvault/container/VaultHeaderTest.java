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
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VaultHeaderTest {

    @Test
    void constructorCreateValidHeader() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = SecureRandomProvider.generateIv();

        VaultHeader header = new VaultHeader(salt, iv);

        assertEquals(VaultHeader.CURRENT_VERSION, header.getVersion());
        assertEquals(0, header.getFlags());
        assertArrayEquals(salt, header.getSalt());
        assertArrayEquals(iv, header.getHeaderIv());
    }

    @Test
    void toBytesProducesCorrectSize() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = SecureRandomProvider.generateIv();
        VaultHeader header = new VaultHeader(salt, iv);

        byte[] bytes = header.toBytes();

        assertEquals(VaultHeader.HEADER_SIZE, bytes.length);
    }

    @Test
    void toBytesStartsWithMagic() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = SecureRandomProvider.generateIv();
        VaultHeader header = new VaultHeader(salt, iv);

        byte[] bytes = header.toBytes();

        byte[] magic = Arrays.copyOfRange(bytes, 0, 8);
        assertArrayEquals(VaultHeader.MAGIC, magic);
    }

    @Test
    void parseRoundTrip() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = SecureRandomProvider.generateIv();
        VaultHeader original = new VaultHeader(salt, iv);

        byte[] bytes = original.toBytes();
        VaultHeader parsed = VaultHeader.parse(bytes);

        assertEquals(original.getVersion(), parsed.getVersion());
        assertEquals(original.getFlags(), parsed.getFlags());
        assertArrayEquals(original.getSalt(), parsed.getSalt());
        assertArrayEquals(original.getHeaderIv(), parsed.getHeaderIv());
    }

    @Test
    void parseRejectsInvalidMagic() {
        byte[] data = new byte[VaultHeader.HEADER_SIZE];
        Arrays.fill(data, (byte) 0);

        assertThrows(VaultException.class, () -> VaultHeader.parse(data));
    }

    @Test
    void parseRejectsShortData() {
        byte[] data = new byte[32];

        assertThrows(VaultException.class, () -> VaultHeader.parse(data));
    }

    @Test
    void parseRejectsNullData() {
        assertThrows(VaultException.class, () -> VaultHeader.parse(null));
    }

    @Test
    void constructorRejectsNullSalt() {
        byte[] iv = SecureRandomProvider.generateIv();

        assertThrows(IllegalArgumentException.class, () -> new VaultHeader(null, iv));
    }

    @Test
    void constructorRejectsWrongSaltSize() {
        byte[] salt = new byte[16];
        byte[] iv = SecureRandomProvider.generateIv();

        assertThrows(IllegalArgumentException.class, () -> new VaultHeader(salt, iv));
    }

    @Test
    void constructorRejectsNullIv() {
        byte[] salt = SecureRandomProvider.generateSalt();

        assertThrows(IllegalArgumentException.class, () -> new VaultHeader(salt, null));
    }

    @Test
    void constructorRejectsWrongIvSize() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = new byte[16];

        assertThrows(IllegalArgumentException.class, () -> new VaultHeader(salt, iv));
    }

    @Test
    void getSaltReturnsCopy() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = SecureRandomProvider.generateIv();
        VaultHeader header = new VaultHeader(salt, iv);

        byte[] retrieved = header.getSalt();
        retrieved[0] ^= 0xFF;

        assertFalse(Arrays.equals(retrieved, header.getSalt()));
    }

    @Test
    void getHeaderIvReturnsCopy() {
        byte[] salt = SecureRandomProvider.generateSalt();
        byte[] iv = SecureRandomProvider.generateIv();
        VaultHeader header = new VaultHeader(salt, iv);

        byte[] retrieved = header.getHeaderIv();
        retrieved[0] ^= 0xFF;

        assertFalse(Arrays.equals(retrieved, header.getHeaderIv()));
    }
}
