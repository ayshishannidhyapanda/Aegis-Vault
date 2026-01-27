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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class CipherRegistry {

    private static final Map<String, Supplier<CipherProvider>> PROVIDERS = new LinkedHashMap<>();
    private static final Map<String, Supplier<CipherProvider>> EXPERIMENTAL_PROVIDERS = new LinkedHashMap<>();
    private static final Map<String, Supplier<CipherProvider>> CASCADE_PROVIDERS = new LinkedHashMap<>();

    private static volatile boolean experimentalEnabled = true;

    static {
        PROVIDERS.put("AES", AesGcmCipherProvider::new);

        EXPERIMENTAL_PROVIDERS.put("Serpent", SerpentGcmCipherProvider::new);
        EXPERIMENTAL_PROVIDERS.put("Twofish", TwofishGcmCipherProvider::new);
        EXPERIMENTAL_PROVIDERS.put("Camellia", CamelliaGcmCipherProvider::new);
        EXPERIMENTAL_PROVIDERS.put("Kuznyechik", KuznyechikGcmCipherProvider::new);

        for (String cascadeId : CascadeCipherProvider.getAllCascadeIds()) {
            CASCADE_PROVIDERS.put(cascadeId, () -> CascadeCipherProvider.create(cascadeId));
        }
    }

    private CipherRegistry() {
    }

    public static CipherProvider getDefault() {
        return new AesGcmCipherProvider();
    }

    public static CipherProvider get(String algorithmId) {
        if (algorithmId == null || algorithmId.isBlank()) {
            return getDefault();
        }

        Supplier<CipherProvider> supplier = PROVIDERS.get(algorithmId);
        if (supplier != null) {
            return supplier.get();
        }

        if (experimentalEnabled) {
            supplier = EXPERIMENTAL_PROVIDERS.get(algorithmId);
            if (supplier != null) {
                return supplier.get();
            }

            supplier = CASCADE_PROVIDERS.get(algorithmId);
            if (supplier != null) {
                return supplier.get();
            }
        }

        throw new IllegalArgumentException(
                "Unknown or disabled cipher algorithm: " + algorithmId +
                        (experimentalEnabled ? "" : " (experimental ciphers are disabled)"));
    }

    public static boolean isExperimentalEnabled() {
        return experimentalEnabled;
    }

    public static void enableExperimental(boolean enabled) {
        if (enabled) {
            System.err.println("[SECURITY WARNING] Experimental ciphers enabled. " +
                    "Security audit findings do NOT apply to experimental modes.");
        }
        experimentalEnabled = enabled;
    }

    public static List<String> getAllSingleCiphers() {
        List<String> ciphers = new ArrayList<>();
        ciphers.addAll(PROVIDERS.keySet());
        if (experimentalEnabled) {
            ciphers.addAll(EXPERIMENTAL_PROVIDERS.keySet());
        }
        return ciphers;
    }

    public static List<String> getAllCascadeCiphers() {
        if (experimentalEnabled) {
            return new ArrayList<>(CASCADE_PROVIDERS.keySet());
        }
        return Collections.emptyList();
    }

    public static List<String> getAllCiphers() {
        List<String> all = new ArrayList<>();
        all.addAll(getAllSingleCiphers());
        all.addAll(getAllCascadeCiphers());
        return all;
    }

    public static Set<String> getStandardAlgorithms() {
        return Collections.unmodifiableSet(PROVIDERS.keySet());
    }

    public static Set<String> getExperimentalAlgorithms() {
        return Collections.unmodifiableSet(EXPERIMENTAL_PROVIDERS.keySet());
    }

    public static boolean isExperimental(String algorithmId) {
        return EXPERIMENTAL_PROVIDERS.containsKey(algorithmId) ||
               CASCADE_PROVIDERS.containsKey(algorithmId);
    }

    public static boolean isCascade(String algorithmId) {
        return CASCADE_PROVIDERS.containsKey(algorithmId);
    }

    public static boolean isAvailable(String algorithmId) {
        if (PROVIDERS.containsKey(algorithmId)) {
            return true;
        }
        return experimentalEnabled &&
               (EXPERIMENTAL_PROVIDERS.containsKey(algorithmId) ||
                CASCADE_PROVIDERS.containsKey(algorithmId));
    }

    static void resetForTesting() {
        experimentalEnabled = true;
    }
}
