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

public abstract class ExperimentalCipherProvider implements CipherProvider {

    private static final String EXPERIMENTAL_WARNING =
            "WARNING: This cipher provider is EXPERIMENTAL and has NOT been security audited. " +
            "Do NOT use for production data. The security guarantees of SECURITY_AUDIT.md " +
            "do NOT apply when experimental ciphers are enabled.";

    protected ExperimentalCipherProvider() {
        logExperimentalWarning();
    }

    @Override
    public final boolean isExperimental() {
        return true;
    }

    protected void logExperimentalWarning() {
        System.err.println("[EXPERIMENTAL] " + getAlgorithmIdentifier() + ": " + EXPERIMENTAL_WARNING);
    }

    protected abstract byte[] doEncrypt(byte[] plaintext, byte[] key, byte[] iv, byte[] aad);

    protected abstract byte[] doDecrypt(byte[] ciphertext, byte[] key, byte[] iv, byte[] aad);

    protected abstract byte[] generateIv();

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad) {
        validateKey(key);
        validatePlaintext(plaintext);

        byte[] iv = generateIv();
        byte[] ciphertextWithTag = doEncrypt(plaintext, key, iv, aad);

        byte[] result = new byte[iv.length + ciphertextWithTag.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(ciphertextWithTag, 0, result, iv.length, ciphertextWithTag.length);

        return result;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] aad) {
        validateKey(key);
        validateCiphertext(ciphertext);

        int ivLen = getIvLengthBytes();
        byte[] iv = new byte[ivLen];
        System.arraycopy(ciphertext, 0, iv, 0, ivLen);

        byte[] ciphertextWithTag = new byte[ciphertext.length - ivLen];
        System.arraycopy(ciphertext, ivLen, ciphertextWithTag, 0, ciphertextWithTag.length);

        return doDecrypt(ciphertextWithTag, key, iv, aad);
    }
}
