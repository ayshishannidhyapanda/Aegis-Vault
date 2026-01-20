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
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmCipherTest {

    @Test
    void encryptDecryptRoundTrip() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = "Hello, AegisVault!".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key);
        byte[] decrypted = AesGcmCipher.decrypt(ciphertext, key);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void encryptDecryptWithAad() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = "Secret data".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "header-data".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key, aad);
        byte[] decrypted = AesGcmCipher.decrypt(ciphertext, key, aad);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void decryptWithWrongAadFails() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = "Secret data".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "correct-aad".getBytes(StandardCharsets.UTF_8);
        byte[] wrongAad = "wrong-aad".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key, aad);

        assertThrows(CryptoException.class, () -> AesGcmCipher.decrypt(ciphertext, key, wrongAad));
    }

    @Test
    void ciphertextContainsIvPlusCiphertextPlusTag() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = "Test".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key);

        int expectedMinLength = SecureRandomProvider.IV_SIZE_BYTES + plaintext.length + 16;
        assertTrue(ciphertext.length >= expectedMinLength);
    }

    @Test
    void differentEncryptionsProduceDifferentCiphertext() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = "Same plaintext".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext1 = AesGcmCipher.encrypt(plaintext, key);
        byte[] ciphertext2 = AesGcmCipher.encrypt(plaintext, key);

        assertFalse(Arrays.equals(ciphertext1, ciphertext2));
    }

    @Test
    void decryptWithWrongKeyFails() {
        byte[] key1 = SecureRandomProvider.generateKey();
        byte[] key2 = SecureRandomProvider.generateKey();
        byte[] plaintext = "Secret".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key1);

        assertThrows(CryptoException.class, () -> AesGcmCipher.decrypt(ciphertext, key2));
    }

    @Test
    void tamperedCiphertextFailsAuthentication() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = "Integrity test".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key);
        ciphertext[ciphertext.length - 1] ^= 0xFF;

        assertThrows(CryptoException.class, () -> AesGcmCipher.decrypt(ciphertext, key));
    }

    @Test
    void nullPlaintextThrows() {
        byte[] key = SecureRandomProvider.generateKey();

        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.encrypt(null, key));
    }

    @Test
    void nullKeyThrows() {
        byte[] plaintext = "Test".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.encrypt(plaintext, null));
    }

    @Test
    void wrongKeySizeThrows() {
        byte[] plaintext = "Test".getBytes(StandardCharsets.UTF_8);
        byte[] shortKey = new byte[16];

        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.encrypt(plaintext, shortKey));
    }

    @Test
    void ciphertextTooShortThrows() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] shortCiphertext = new byte[10];

        assertThrows(IllegalArgumentException.class, () -> AesGcmCipher.decrypt(shortCiphertext, key));
    }

    @Test
    void emptyPlaintextEncryptsSuccessfully() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = new byte[0];

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key);
        byte[] decrypted = AesGcmCipher.decrypt(ciphertext, key);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    void largePlaintextEncryptsSuccessfully() {
        byte[] key = SecureRandomProvider.generateKey();
        byte[] plaintext = new byte[1024 * 1024];
        Arrays.fill(plaintext, (byte) 0xAB);

        byte[] ciphertext = AesGcmCipher.encrypt(plaintext, key);
        byte[] decrypted = AesGcmCipher.decrypt(ciphertext, key);

        assertArrayEquals(plaintext, decrypted);
    }
}
