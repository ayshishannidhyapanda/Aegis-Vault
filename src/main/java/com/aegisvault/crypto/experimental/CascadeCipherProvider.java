/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

import com.aegisvault.crypto.Argon2KeyDeriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CascadeCipherProvider implements CipherProvider {

    public static final String CASCADE_AES_TWOFISH = "AES(Twofish)";
    public static final String CASCADE_AES_TWOFISH_SERPENT = "AES(Twofish(Serpent))";
    public static final String CASCADE_SERPENT_AES = "Serpent(AES)";
    public static final String CASCADE_SERPENT_TWOFISH_AES = "Serpent(Twofish(AES))";
    public static final String CASCADE_TWOFISH_SERPENT = "Twofish(Serpent)";
    public static final String CASCADE_CAMELLIA_KUZNYECHIK = "Camellia(Kuznyechik)";
    public static final String CASCADE_CAMELLIA_SERPENT = "Camellia(Serpent)";
    public static final String CASCADE_KUZNYECHIK_AES = "Kuznyechik(AES)";
    public static final String CASCADE_KUZNYECHIK_SERPENT_CAMELLIA = "Kuznyechik(Serpent(Camellia))";
    public static final String CASCADE_KUZNYECHIK_TWOFISH = "Kuznyechik(Twofish)";

    private static final String EXPERIMENTAL_WARNING =
            "WARNING: Cipher cascades are HIGHLY EXPERIMENTAL. " +
            "Cascading does NOT necessarily increase security and may introduce bugs. " +
            "SECURITY_AUDIT.md does NOT apply to cascade mode.";

    private final String cascadeId;
    private final List<CipherProvider> layers;

    private CascadeCipherProvider(String cascadeId, List<CipherProvider> layers) {
        this.cascadeId = cascadeId;
        this.layers = List.copyOf(layers);
        logWarning();
    }

    public static CascadeCipherProvider create(String cascadeId) {
        List<CipherProvider> layers = switch (cascadeId) {
            case CASCADE_AES_TWOFISH -> Arrays.asList(
                    new AesGcmCipherProvider(), new TwofishGcmCipherProvider());
            case CASCADE_AES_TWOFISH_SERPENT -> Arrays.asList(
                    new AesGcmCipherProvider(), new TwofishGcmCipherProvider(), new SerpentGcmCipherProvider());
            case CASCADE_SERPENT_AES -> Arrays.asList(
                    new SerpentGcmCipherProvider(), new AesGcmCipherProvider());
            case CASCADE_SERPENT_TWOFISH_AES -> Arrays.asList(
                    new SerpentGcmCipherProvider(), new TwofishGcmCipherProvider(), new AesGcmCipherProvider());
            case CASCADE_TWOFISH_SERPENT -> Arrays.asList(
                    new TwofishGcmCipherProvider(), new SerpentGcmCipherProvider());
            case CASCADE_CAMELLIA_KUZNYECHIK -> Arrays.asList(
                    new CamelliaGcmCipherProvider(), new KuznyechikGcmCipherProvider());
            case CASCADE_CAMELLIA_SERPENT -> Arrays.asList(
                    new CamelliaGcmCipherProvider(), new SerpentGcmCipherProvider());
            case CASCADE_KUZNYECHIK_AES -> Arrays.asList(
                    new KuznyechikGcmCipherProvider(), new AesGcmCipherProvider());
            case CASCADE_KUZNYECHIK_SERPENT_CAMELLIA -> Arrays.asList(
                    new KuznyechikGcmCipherProvider(), new SerpentGcmCipherProvider(), new CamelliaGcmCipherProvider());
            case CASCADE_KUZNYECHIK_TWOFISH -> Arrays.asList(
                    new KuznyechikGcmCipherProvider(), new TwofishGcmCipherProvider());
            default -> throw new IllegalArgumentException("Unknown cascade ID: " + cascadeId);
        };
        return new CascadeCipherProvider(cascadeId, layers);
    }

    public static List<String> getAllCascadeIds() {
        return Arrays.asList(
                CASCADE_AES_TWOFISH,
                CASCADE_AES_TWOFISH_SERPENT,
                CASCADE_SERPENT_AES,
                CASCADE_SERPENT_TWOFISH_AES,
                CASCADE_TWOFISH_SERPENT,
                CASCADE_CAMELLIA_KUZNYECHIK,
                CASCADE_CAMELLIA_SERPENT,
                CASCADE_KUZNYECHIK_AES,
                CASCADE_KUZNYECHIK_SERPENT_CAMELLIA,
                CASCADE_KUZNYECHIK_TWOFISH
        );
    }

    private void logWarning() {
        System.err.println("[EXPERIMENTAL CASCADE] " + cascadeId + ": " + EXPERIMENTAL_WARNING);
    }

    @Override
    public String getAlgorithmIdentifier() {
        return cascadeId;
    }

    @Override
    public int getKeyLengthBytes() {
        return layers.size() * 32;
    }

    @Override
    public int getIvLengthBytes() {
        return 12;
    }

    @Override
    public int getTagLengthBytes() {
        return 16;
    }

    @Override
    public boolean isExperimental() {
        return true;
    }

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] aad) {
        validateKey(key);
        validatePlaintext(plaintext);

        byte[] data = plaintext;
        for (int i = 0; i < layers.size(); i++) {
            CipherProvider layer = layers.get(i);
            byte[] layerKey = deriveLayerKey(key, i);
            try {
                data = layer.encrypt(data, layerKey, aad);
            } finally {
                Argon2KeyDeriver.zeroBytes(layerKey);
            }
        }
        return data;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] aad) {
        validateKey(key);
        validateCiphertext(ciphertext);

        byte[] data = ciphertext;
        for (int i = layers.size() - 1; i >= 0; i--) {
            CipherProvider layer = layers.get(i);
            byte[] layerKey = deriveLayerKey(key, i);
            try {
                data = layer.decrypt(data, layerKey, aad);
            } finally {
                Argon2KeyDeriver.zeroBytes(layerKey);
            }
        }
        return data;
    }

    private byte[] deriveLayerKey(byte[] masterKey, int layerIndex) {
        int start = layerIndex * 32;
        byte[] layerKey = new byte[32];
        System.arraycopy(masterKey, start, layerKey, 0, 32);
        return layerKey;
    }

    public List<String> getLayerAlgorithms() {
        List<String> algorithms = new ArrayList<>();
        for (CipherProvider layer : layers) {
            algorithms.add(layer.getAlgorithmIdentifier());
        }
        return algorithms;
    }

    public int getLayerCount() {
        return layers.size();
    }
}
