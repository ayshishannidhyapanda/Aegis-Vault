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
package com.aegisvault.crypto.experimental;

import com.aegisvault.crypto.AesGcmCipher;
import com.aegisvault.crypto.Argon2KeyDeriver;
import com.aegisvault.crypto.SecureRandomProvider;
import com.aegisvault.exception.CryptoException;

import java.util.Arrays;

public final class CryptoSelfTest {

    private static final byte[] TEST_KEY = hexToBytes(
            "000102030405060708090A0B0C0D0E0F" +
            "101112131415161718191A1B1C1D1E1F"
    );

    private static final byte[] TEST_PLAINTEXT = hexToBytes(
            "48656C6C6F2C20576F726C6421"
    );

    private static final byte[] TEST_SALT = hexToBytes(
            "000102030405060708090A0B0C0D0E0F" +
            "101112131415161718191A1B1C1D1E1F"
    );

    private static final char[] TEST_PASSWORD = "TestPassword123!".toCharArray();

    private static volatile boolean selfTestPassed = false;

    private CryptoSelfTest() {
    }

    public static void runOnStartup() {
        System.out.println("[SELF-TEST] Running cryptographic self-tests...");

        try {
            testAesGcmRoundTrip();
            testArgon2idDerivation();
            testSecureRandomQuality();
            selfTestPassed = true;
            System.out.println("[SELF-TEST] All cryptographic self-tests PASSED");
        } catch (Exception e) {
            selfTestPassed = false;
            System.err.println("[SELF-TEST] CRITICAL: Cryptographic self-test FAILED: " + e.getMessage());
            throw new CryptoException("Cryptographic self-test failed. Application cannot proceed safely.", e);
        }
    }

    public static boolean hasSelfTestPassed() {
        return selfTestPassed;
    }

    private static void testAesGcmRoundTrip() {
        byte[] encrypted = AesGcmCipher.encrypt(TEST_PLAINTEXT, TEST_KEY);
        byte[] decrypted = AesGcmCipher.decrypt(encrypted, TEST_KEY);

        if (!Arrays.equals(TEST_PLAINTEXT, decrypted)) {
            throw new CryptoException("AES-GCM round-trip test failed: plaintext mismatch");
        }

        if (encrypted.length <= TEST_PLAINTEXT.length) {
            throw new CryptoException("AES-GCM test failed: ciphertext not longer than plaintext");
        }

        System.out.println("[SELF-TEST] AES-256-GCM round-trip: PASSED");
    }

    private static void testArgon2idDerivation() {
        char[] password = TEST_PASSWORD.clone();
        byte[] salt = TEST_SALT.clone();

        byte[] key1 = Argon2KeyDeriver.deriveKey(password, salt);

        password = TEST_PASSWORD.clone();
        salt = TEST_SALT.clone();
        byte[] key2 = Argon2KeyDeriver.deriveKey(password, salt);

        if (!Arrays.equals(key1, key2)) {
            throw new CryptoException("Argon2id test failed: non-deterministic output");
        }

        if (key1.length != 32) {
            throw new CryptoException("Argon2id test failed: incorrect key length");
        }

        boolean allZero = true;
        for (byte b : key1) {
            if (b != 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            throw new CryptoException("Argon2id test failed: derived key is all zeros");
        }

        System.out.println("[SELF-TEST] Argon2id key derivation: PASSED");
    }

    private static void testSecureRandomQuality() {
        byte[] random1 = SecureRandomProvider.generateKey();
        byte[] random2 = SecureRandomProvider.generateKey();

        if (Arrays.equals(random1, random2)) {
            throw new CryptoException("SecureRandom test failed: repeated output detected");
        }

        int onesCount = 0;
        for (byte b : random1) {
            onesCount += Integer.bitCount(b & 0xFF);
        }

        double ratio = (double) onesCount / (random1.length * 8);
        if (ratio < 0.3 || ratio > 0.7) {
            System.err.println("[SELF-TEST] WARNING: SecureRandom bit distribution unusual: " + ratio);
        }

        System.out.println("[SELF-TEST] SecureRandom quality: PASSED");
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
