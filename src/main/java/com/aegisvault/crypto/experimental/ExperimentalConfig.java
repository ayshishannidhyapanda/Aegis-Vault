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

public final class ExperimentalConfig {

    private static final String PREFIX = "aegisvault.experimental.";

    private static volatile boolean globalExperimentalEnabled = false;
    private static volatile boolean serpentEnabled = false;
    private static volatile boolean twofishEnabled = false;
    private static volatile boolean camelliaEnabled = false;
    private static volatile boolean kuznyechikEnabled = false;
    private static volatile boolean cascadesEnabled = false;
    private static volatile boolean alternativeKdfEnabled = false;
    private static volatile boolean mouseEntropyEnabled = false;
    private static volatile boolean selfTestOnStartup = false;

    private ExperimentalConfig() {
    }

    public static void loadFromSystemProperties() {
        globalExperimentalEnabled = getBooleanProperty("ciphers.enabled", false);
        serpentEnabled = getBooleanProperty("cipher.serpent.enabled", false);
        twofishEnabled = getBooleanProperty("cipher.twofish.enabled", false);
        camelliaEnabled = getBooleanProperty("cipher.camellia.enabled", false);
        kuznyechikEnabled = getBooleanProperty("cipher.kuznyechik.enabled", false);
        cascadesEnabled = getBooleanProperty("cascades.enabled", false);
        alternativeKdfEnabled = getBooleanProperty("kdf.alternatives.enabled", false);
        mouseEntropyEnabled = getBooleanProperty("entropy.mouse.enabled", false);
        selfTestOnStartup = getBooleanProperty("crypto.selftest.enabled", false);

        if (isAnyExperimentalEnabled()) {
            printExperimentalWarning();
        }
    }

    private static boolean getBooleanProperty(String suffix, boolean defaultValue) {
        String value = System.getProperty(PREFIX + suffix);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public static boolean isGlobalExperimentalEnabled() {
        return globalExperimentalEnabled;
    }

    public static boolean isSerpentEnabled() {
        return globalExperimentalEnabled && serpentEnabled;
    }

    public static boolean isTwofishEnabled() {
        return globalExperimentalEnabled && twofishEnabled;
    }

    public static boolean isCamelliaEnabled() {
        return globalExperimentalEnabled && camelliaEnabled;
    }

    public static boolean isKuznyechikEnabled() {
        return globalExperimentalEnabled && kuznyechikEnabled;
    }

    public static boolean isCascadesEnabled() {
        return globalExperimentalEnabled && cascadesEnabled;
    }

    public static boolean isAlternativeKdfEnabled() {
        return globalExperimentalEnabled && alternativeKdfEnabled;
    }

    public static boolean isMouseEntropyEnabled() {
        return mouseEntropyEnabled;
    }

    public static boolean isSelfTestOnStartup() {
        return selfTestOnStartup;
    }

    public static boolean isAnyExperimentalEnabled() {
        return globalExperimentalEnabled ||
               mouseEntropyEnabled ||
               cascadesEnabled ||
               alternativeKdfEnabled;
    }

    private static void printExperimentalWarning() {
        System.err.println();
        System.err.println("╔══════════════════════════════════════════════════════════════════╗");
        System.err.println("║  ⚠️  EXPERIMENTAL CRYPTOGRAPHY MODE ACTIVE                       ║");
        System.err.println("╠══════════════════════════════════════════════════════════════════╣");
        System.err.println("║  One or more experimental cryptographic features are enabled.    ║");
        System.err.println("║                                                                  ║");
        System.err.println("║  • These features have NOT been security audited                 ║");
        System.err.println("║  • They may contain implementation bugs                          ║");
        System.err.println("║  • SECURITY_AUDIT.md findings do NOT apply                       ║");
        System.err.println("║  • Do NOT use for production or sensitive data                   ║");
        System.err.println("║                                                                  ║");
        System.err.println("║  For production use, disable all experimental features.          ║");
        System.err.println("╚══════════════════════════════════════════════════════════════════╝");
        System.err.println();
    }

    public static String getActiveExperimentalFeatures() {
        StringBuilder sb = new StringBuilder();
        if (serpentEnabled) sb.append("Serpent-256-GCM, ");
        if (twofishEnabled) sb.append("Twofish-256-GCM, ");
        if (camelliaEnabled) sb.append("Camellia-256-GCM, ");
        if (kuznyechikEnabled) sb.append("Kuznyechik-256-GCM, ");
        if (cascadesEnabled) sb.append("Cipher Cascades, ");
        if (alternativeKdfEnabled) sb.append("Alternative KDFs, ");
        if (mouseEntropyEnabled) sb.append("Mouse Entropy, ");

        if (sb.isEmpty()) {
            return "None";
        }

        return sb.substring(0, sb.length() - 2);
    }

    static void resetForTesting() {
        globalExperimentalEnabled = false;
        serpentEnabled = false;
        twofishEnabled = false;
        camelliaEnabled = false;
        kuznyechikEnabled = false;
        cascadesEnabled = false;
        alternativeKdfEnabled = false;
        mouseEntropyEnabled = false;
        selfTestOnStartup = false;
    }
}
