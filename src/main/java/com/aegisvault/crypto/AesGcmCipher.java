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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public final class AesGcmCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int TAG_LENGTH_BITS = 128;

    private AesGcmCipher() {
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key) {
        return encrypt(plaintext, key, null);
    }

    public static byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad) {
        validateKey(key);
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext must not be null");
        }

        try {
            byte[] iv = SecureRandomProvider.generateIv();
            SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }

            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            byte[] result = new byte[iv.length + ciphertextWithTag.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertextWithTag, 0, result, iv.length, ciphertextWithTag.length);

            return result;
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key) {
        return decrypt(ciphertext, key, null);
    }

    public static byte[] decrypt(byte[] ciphertext, byte[] key, byte[] aad) {
        validateKey(key);
        int minLength = SecureRandomProvider.IV_SIZE_BYTES + TAG_LENGTH_BITS / 8;
        if (ciphertext == null || ciphertext.length < minLength) {
            throw new IllegalArgumentException("Ciphertext too short or null");
        }

        try {
            byte[] iv = Arrays.copyOfRange(ciphertext, 0, SecureRandomProvider.IV_SIZE_BYTES);
            byte[] ciphertextWithTag = Arrays.copyOfRange(ciphertext, SecureRandomProvider.IV_SIZE_BYTES, ciphertext.length);

            SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            if (aad != null && aad.length > 0) {
                cipher.updateAAD(aad);
            }

            return cipher.doFinal(ciphertextWithTag);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new CryptoException("Authentication failed - data may be tampered", e);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }

    private static void validateKey(byte[] key) {
        if (key == null || key.length != SecureRandomProvider.KEY_SIZE_BYTES) {
            throw new IllegalArgumentException(
                "Key must be exactly " + SecureRandomProvider.KEY_SIZE_BYTES + " bytes (256 bits)");
        }
    }
}
