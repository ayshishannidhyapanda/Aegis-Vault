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

import java.util.Map;

public interface KeyDerivationFunction {

    String getIdentifier();

    boolean isExperimental();

    byte[] deriveKey(char[] password, byte[] salt, int outputLengthBytes);

    int getRecommendedSaltLengthBytes();

    Map<String, Object> getParameters();

    default void validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
    }

    default void validateSalt(byte[] salt) {
        if (salt == null || salt.length < 16) {
            throw new IllegalArgumentException("Salt must be at least 16 bytes");
        }
    }

    default void validateOutputLength(int outputLengthBytes) {
        if (outputLengthBytes < 16 || outputLengthBytes > 64) {
            throw new IllegalArgumentException(
                    "Output length must be between 16 and 64 bytes");
        }
    }
}
