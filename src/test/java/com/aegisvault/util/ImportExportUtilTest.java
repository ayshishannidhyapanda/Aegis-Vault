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

import com.aegisvault.service.VaultService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImportExportUtilTest {

    @TempDir
    Path tempDir;

    private VaultService service;
    private Path vaultPath;

    @BeforeEach
    void setUp() {
        vaultPath = tempDir.resolve("test.avj");
        service = new VaultService();
        service.createVault(vaultPath, "password".toCharArray());
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.close();
        }
    }

    @Test
    void importSingleFile() throws Exception {
        Path sourceFile = tempDir.resolve("test.txt");
        Files.writeString(sourceFile, "Hello, World!");

        int count = ImportExportUtil.importFile(service, sourceFile, "/");

        assertEquals(1, count);
        assertTrue(service.exists("/test.txt"));
        assertArrayEquals("Hello, World!".getBytes(StandardCharsets.UTF_8), service.readFile("/test.txt"));
    }

    @Test
    void importFileToSubdirectory() throws Exception {
        service.createDirectory("/docs");
        Path sourceFile = tempDir.resolve("readme.txt");
        Files.writeString(sourceFile, "Content");

        ImportExportUtil.importFile(service, sourceFile, "/docs");

        assertTrue(service.exists("/docs/readme.txt"));
    }

    @Test
    void importDirectory() throws Exception {
        Path sourceDir = tempDir.resolve("mydir");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("file1.txt"), "File 1");
        Files.createDirectories(sourceDir.resolve("subdir"));
        Files.writeString(sourceDir.resolve("subdir/file2.txt"), "File 2");

        int count = ImportExportUtil.importDirectory(service, sourceDir, "/");

        assertEquals(4, count);
        assertTrue(service.exists("/mydir"));
        assertTrue(service.exists("/mydir/file1.txt"));
        assertTrue(service.exists("/mydir/subdir"));
        assertTrue(service.exists("/mydir/subdir/file2.txt"));
    }

    @Test
    void exportSingleFile() throws Exception {
        service.createFile("/export.txt", "Export content".getBytes(StandardCharsets.UTF_8));

        Path targetDir = tempDir.resolve("export_target");
        Files.createDirectories(targetDir);

        int count = ImportExportUtil.exportFile(service, "/export.txt", targetDir);

        assertEquals(1, count);
        Path exported = targetDir.resolve("export.txt");
        assertTrue(Files.exists(exported));
        assertEquals("Export content", Files.readString(exported));
    }

    @Test
    void exportDirectory() throws Exception {
        service.createDirectory("/exportdir");
        service.createFile("/exportdir/a.txt", "A".getBytes(StandardCharsets.UTF_8));
        service.createDirectory("/exportdir/sub");
        service.createFile("/exportdir/sub/b.txt", "B".getBytes(StandardCharsets.UTF_8));

        Path targetDir = tempDir.resolve("export_target");
        Files.createDirectories(targetDir);

        int count = ImportExportUtil.exportDirectory(service, "/exportdir", targetDir);

        assertEquals(4, count);
        assertTrue(Files.exists(targetDir.resolve("exportdir")));
        assertTrue(Files.exists(targetDir.resolve("exportdir/a.txt")));
        assertTrue(Files.exists(targetDir.resolve("exportdir/sub")));
        assertTrue(Files.exists(targetDir.resolve("exportdir/sub/b.txt")));
    }

    @Test
    void importNonExistentFileThrows() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");

        assertThrows(Exception.class, () -> ImportExportUtil.importFile(service, nonExistent, "/"));
    }

    @Test
    void exportNonExistentPathThrows() throws Exception {
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(targetDir);

        assertThrows(IllegalArgumentException.class,
            () -> ImportExportUtil.exportFile(service, "/nonexistent", targetDir));
    }
}
