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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BackupUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void createBackupSameDirectory() throws Exception {
        Path vault = tempDir.resolve("vault.avj");
        Files.writeString(vault, "vault content");

        Path backup = BackupUtil.createBackup(vault);

        assertTrue(Files.exists(backup));
        assertTrue(backup.getFileName().toString().contains(".bak"));
        assertEquals("vault content", Files.readString(backup));
    }

    @Test
    void createBackupToTargetDirectory() throws Exception {
        Path vault = tempDir.resolve("vault.avj");
        Files.writeString(vault, "vault data");

        Path backupDir = tempDir.resolve("backups");

        Path backup = BackupUtil.createBackup(vault, backupDir);

        assertTrue(Files.exists(backup));
        assertTrue(backup.startsWith(backupDir));
        assertEquals("vault data", Files.readString(backup));
    }

    @Test
    void createBackupWithCustomName() throws Exception {
        Path vault = tempDir.resolve("vault.avj");
        Files.writeString(vault, "custom backup");

        Path targetPath = tempDir.resolve("my_backup.avj.bak");

        Path backup = BackupUtil.createBackupWithName(vault, targetPath);

        assertEquals(targetPath, backup);
        assertTrue(Files.exists(backup));
        assertEquals("custom backup", Files.readString(backup));
    }

    @Test
    void restoreBackup() throws Exception {
        Path original = tempDir.resolve("original.avj");
        Path backup = tempDir.resolve("backup.avj.bak");

        Files.writeString(original, "original content");
        Files.writeString(backup, "backup content");

        BackupUtil.restoreBackup(backup, original);

        assertEquals("backup content", Files.readString(original));
    }

    @Test
    void getBackupSize() throws Exception {
        Path backup = tempDir.resolve("backup.avj.bak");
        byte[] content = new byte[1024];
        Files.write(backup, content);

        long size = BackupUtil.getBackupSize(backup);

        assertEquals(1024, size);
    }

    @Test
    void createBackupNonExistentVaultThrows() {
        Path nonExistent = tempDir.resolve("nonexistent.avj");

        assertThrows(IllegalArgumentException.class, () -> BackupUtil.createBackup(nonExistent));
    }

    @Test
    void restoreNonExistentBackupThrows() {
        Path nonExistent = tempDir.resolve("nonexistent.bak");
        Path target = tempDir.resolve("target.avj");

        assertThrows(IllegalArgumentException.class, () -> BackupUtil.restoreBackup(nonExistent, target));
    }
}
