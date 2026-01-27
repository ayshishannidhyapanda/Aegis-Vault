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

public interface CipherProvider {

    String getAlgorithmIdentifier();

    int getKeyLengthBytes();

    int getIvLengthBytes();

    int getTagLengthBytes();

    boolean isExperimental();

    byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad);

    byte[] decrypt(byte[] ciphertext, byte[] key, byte[] aad);

    default byte[] encrypt(byte[] plaintext, byte[] key) {
        return encrypt(plaintext, key, null);
    }

    default byte[] decrypt(byte[] ciphertext, byte[] key) {
        return decrypt(ciphertext, key, null);
    }

    default void validateKey(byte[] key) {
        if (key == null || key.length != getKeyLengthBytes()) {
            throw new IllegalArgumentException(
                    "Key must be exactly " + getKeyLengthBytes() + " bytes");
        }
    }

    default void validatePlaintext(byte[] plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext must not be null");
        }
    }

    default void validateCiphertext(byte[] ciphertext) {
        int minLength = getIvLengthBytes() + getTagLengthBytes();
        if (ciphertext == null || ciphertext.length < minLength) {
            throw new IllegalArgumentException(
                    "Ciphertext too short or null (minimum " + minLength + " bytes)");
        }
    }
}
