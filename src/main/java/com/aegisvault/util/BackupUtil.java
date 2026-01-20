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
package com.aegisvault.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BackupUtil {

    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private BackupUtil() {
    }

    public static Path createBackup(Path vaultPath) throws IOException {
        if (!Files.exists(vaultPath)) {
            throw new IllegalArgumentException("Vault file does not exist: " + vaultPath);
        }

        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
        String fileName = vaultPath.getFileName().toString();
        String backupName = fileName + "." + timestamp + ".bak";
        Path backupPath = vaultPath.getParent().resolve(backupName);

        Files.copy(vaultPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    public static Path createBackup(Path vaultPath, Path targetDir) throws IOException {
        if (!Files.exists(vaultPath)) {
            throw new IllegalArgumentException("Vault file does not exist: " + vaultPath);
        }

        if (!Files.isDirectory(targetDir)) {
            Files.createDirectories(targetDir);
        }

        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
        String fileName = vaultPath.getFileName().toString();
        String backupName = fileName + "." + timestamp + ".bak";
        Path backupPath = targetDir.resolve(backupName);

        Files.copy(vaultPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    public static Path createBackupWithName(Path vaultPath, Path targetPath) throws IOException {
        if (!Files.exists(vaultPath)) {
            throw new IllegalArgumentException("Vault file does not exist: " + vaultPath);
        }

        Files.copy(vaultPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    public static void restoreBackup(Path backupPath, Path vaultPath) throws IOException {
        if (!Files.exists(backupPath)) {
            throw new IllegalArgumentException("Backup file does not exist: " + backupPath);
        }

        Files.copy(backupPath, vaultPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public static long getBackupSize(Path backupPath) throws IOException {
        return Files.size(backupPath);
    }
}
