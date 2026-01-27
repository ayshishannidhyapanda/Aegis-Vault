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

import com.aegisvault.crypto.Argon2KeyDeriver;
import com.aegisvault.crypto.SecureRandomProvider;

import java.util.Map;

public final class Argon2idKdfProvider implements KeyDerivationFunction {

    private static final String IDENTIFIER = "ARGON2ID";
    private static final int MEMORY_COST_KB = 65536;
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 1;

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public byte[] deriveKey(char[] password, byte[] salt, int outputLengthBytes) {
        validatePassword(password);
        validateSalt(salt);
        validateOutputLength(outputLengthBytes);

        return Argon2KeyDeriver.deriveKey(password, salt);
    }

    @Override
    public int getRecommendedSaltLengthBytes() {
        return SecureRandomProvider.SALT_SIZE_BYTES;
    }

    @Override
    public Map<String, Object> getParameters() {
        return Map.of(
                "algorithm", "Argon2id",
                "memoryKB", MEMORY_COST_KB,
                "iterations", ITERATIONS,
                "parallelism", PARALLELISM
        );
    }
}
