/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

public final class CryptoSettings {

    private static volatile CryptoSettings instance = new CryptoSettings();

    private String selectedCipher = "AES";
    private String selectedHash = "SHA-512";
    private boolean useMouseEntropy = true;
    private byte[] collectedEntropy = null;

    private CryptoSettings() {
    }

    public static CryptoSettings getInstance() {
        return instance;
    }

    public String getSelectedCipher() {
        return selectedCipher;
    }

    public void setSelectedCipher(String cipher) {
        this.selectedCipher = cipher;
    }

    public String getSelectedHash() {
        return selectedHash;
    }

    public void setSelectedHash(String hash) {
        this.selectedHash = hash;
    }

    public boolean isUseMouseEntropy() {
        return useMouseEntropy;
    }

    public void setUseMouseEntropy(boolean useMouseEntropy) {
        this.useMouseEntropy = useMouseEntropy;
    }

    public byte[] getCollectedEntropy() {
        return collectedEntropy;
    }

    public void setCollectedEntropy(byte[] entropy) {
        this.collectedEntropy = entropy;
    }

    public CipherProvider getCipherProvider() {
        return CipherRegistry.get(selectedCipher);
    }

    public HashProvider getHashProvider() {
        return HashProviders.get(selectedHash);
    }

    public boolean isExperimentalCipher() {
        return CipherRegistry.isExperimental(selectedCipher);
    }

    public boolean isCascadeCipher() {
        return CipherRegistry.isCascade(selectedCipher);
    }

    public String getSettingsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cipher: ").append(selectedCipher);
        if (isExperimentalCipher()) {
            sb.append(" [EXPERIMENTAL]");
        }
        sb.append("\nHash: ").append(selectedHash);
        sb.append("\nMouse Entropy: ").append(useMouseEntropy ? "Enabled" : "Disabled");
        if (collectedEntropy != null) {
            sb.append(" (").append(collectedEntropy.length).append(" bytes collected)");
        }
        return sb.toString();
    }

    public void reset() {
        selectedCipher = "AES";
        selectedHash = "SHA-512";
        useMouseEntropy = true;
        collectedEntropy = null;
    }
}
