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
import com.aegisvault.crypto.SecureRandomProvider;

public final class AesGcmCipherProvider implements CipherProvider {

    private static final String ALGORITHM_ID = "AES";
    private static final int KEY_LENGTH = SecureRandomProvider.KEY_SIZE_BYTES;
    private static final int IV_LENGTH = SecureRandomProvider.IV_SIZE_BYTES;
    private static final int TAG_LENGTH = 16;

    @Override
    public String getAlgorithmIdentifier() {
        return ALGORITHM_ID;
    }

    @Override
    public int getKeyLengthBytes() {
        return KEY_LENGTH;
    }

    @Override
    public int getIvLengthBytes() {
        return IV_LENGTH;
    }

    @Override
    public int getTagLengthBytes() {
        return TAG_LENGTH;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad) {
        validateKey(key);
        validatePlaintext(plaintext);
        return AesGcmCipher.encrypt(plaintext, key, aad);
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] aad) {
        validateKey(key);
        validateCiphertext(ciphertext);
        return AesGcmCipher.decrypt(ciphertext, key, aad);
    }
}
