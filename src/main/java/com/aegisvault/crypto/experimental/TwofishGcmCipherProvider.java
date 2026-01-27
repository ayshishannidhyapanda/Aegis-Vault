/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

import com.aegisvault.crypto.SecureRandomProvider;
import com.aegisvault.exception.CryptoException;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public final class TwofishGcmCipherProvider extends ExperimentalCipherProvider {

    private static final String ALGORITHM_ID = "Twofish";
    private static final int KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 16;
    private static final int TAG_LENGTH_BITS = 128;

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
    protected byte[] generateIv() {
        return SecureRandomProvider.generateIv();
    }

    @Override
    protected byte[] doEncrypt(byte[] plaintext, byte[] key, byte[] iv, byte[] aad) {
        try {
            GCMModeCipher cipher = GCMBlockCipher.newInstance(new TwofishEngine());
            AEADParameters params = new AEADParameters(new KeyParameter(key), TAG_LENGTH_BITS, iv, aad);
            cipher.init(true, params);

            byte[] output = new byte[cipher.getOutputSize(plaintext.length)];
            int len = cipher.processBytes(plaintext, 0, plaintext.length, output, 0);
            cipher.doFinal(output, len);

            return output;
        } catch (Exception e) {
            throw new CryptoException("Twofish-GCM encryption failed", e);
        }
    }

    @Override
    protected byte[] doDecrypt(byte[] ciphertext, byte[] key, byte[] iv, byte[] aad) {
        try {
            GCMModeCipher cipher = GCMBlockCipher.newInstance(new TwofishEngine());
            AEADParameters params = new AEADParameters(new KeyParameter(key), TAG_LENGTH_BITS, iv, aad);
            cipher.init(false, params);

            byte[] output = new byte[cipher.getOutputSize(ciphertext.length)];
            int len = cipher.processBytes(ciphertext, 0, ciphertext.length, output, 0);
            cipher.doFinal(output, len);

            return output;
        } catch (Exception e) {
            throw new CryptoException("Twofish-GCM decryption failed - data may be tampered", e);
        }
    }
}
