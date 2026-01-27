/*
 * Copyright (c) 2026 Aegis Vault
 * All rights reserved.
 */
package com.aegisvault.crypto.experimental;

public interface HashProvider {

    String getIdentifier();

    int getOutputLengthBytes();

    boolean isExperimental();

    byte[] hash(byte[] input);

    byte[] hash(byte[] input, byte[] salt);
}
