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
package com.aegisvault.exception;

public class AegisVaultException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AegisVaultException(String message) {
        super(message);
    }

    public AegisVaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
