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
package com.aegisvault.integration;

import com.aegisvault.service.VaultService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VaultPersistenceTest {

    @TempDir
    Path tempDir;

    private VaultService service;
    private Path vaultPath;

    @BeforeEach
    void setUp() {
        vaultPath = tempDir.resolve("persistence-test.avj");
        service = new VaultService();
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.close();
        }
    }

    @Test
    void createVaultAddFileCloseReopenShouldWork() {
        service.createVault(vaultPath, "password123".toCharArray());

        byte[] content = "Test file content".getBytes(StandardCharsets.UTF_8);
        service.createFile("/test.txt", content);

        assertTrue(service.exists("/test.txt"));

        service.close();

        service = new VaultService();
        service.openVault(vaultPath, "password123".toCharArray());

        assertTrue(service.isVaultOpen());
        assertTrue(service.exists("/test.txt"));
        assertArrayEquals(content, service.readFile("/test.txt"));
    }

    @Test
    void createVaultAddMultipleFilesCloseReopenShouldWork() {
        service.createVault(vaultPath, "password".toCharArray());

        service.createDirectory("/docs");
        service.createFile("/docs/file1.txt", "Content 1".getBytes(StandardCharsets.UTF_8));
        service.createFile("/docs/file2.txt", "Content 2".getBytes(StandardCharsets.UTF_8));
        service.createDirectory("/images");
        service.createFile("/images/photo.jpg", new byte[1024]);

        service.close();

        service = new VaultService();
        service.openVault(vaultPath, "password".toCharArray());

        assertTrue(service.exists("/docs"));
        assertTrue(service.exists("/docs/file1.txt"));
        assertTrue(service.exists("/docs/file2.txt"));
        assertTrue(service.exists("/images"));
        assertTrue(service.exists("/images/photo.jpg"));

        assertEquals(2, service.listDirectory("/docs").size());
        assertEquals(1, service.listDirectory("/images").size());
    }

    @Test
    void multipleOpenCloseOperationsShouldWork() {
        service.createVault(vaultPath, "pass".toCharArray());
        service.createFile("/file1.txt", "1".getBytes(StandardCharsets.UTF_8));
        service.close();

        for (int i = 2; i <= 5; i++) {
            service = new VaultService();
            service.openVault(vaultPath, "pass".toCharArray());
            service.createFile("/file" + i + ".txt", String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            service.close();
        }

        service = new VaultService();
        service.openVault(vaultPath, "pass".toCharArray());

        assertEquals(5, service.listDirectory("/").size());
        for (int i = 1; i <= 5; i++) {
            assertTrue(service.exists("/file" + i + ".txt"));
        }
    }

    @Test
    void largeFileShouldPersist() {
        service.createVault(vaultPath, "password".toCharArray());

        byte[] largeContent = new byte[1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }

        service.createFile("/large.bin", largeContent);
        service.close();

        service = new VaultService();
        service.openVault(vaultPath, "password".toCharArray());

        assertTrue(service.exists("/large.bin"));
        byte[] retrieved = service.readFile("/large.bin");
        assertArrayEquals(largeContent, retrieved);
    }

    @Test
    void deleteFileThenReopenShouldNotHaveFile() {
        service.createVault(vaultPath, "password".toCharArray());

        service.createFile("/toDelete.txt", "delete me".getBytes(StandardCharsets.UTF_8));
        assertTrue(service.exists("/toDelete.txt"));

        service.delete("/toDelete.txt");
        assertFalse(service.exists("/toDelete.txt"));

        service.close();

        service = new VaultService();
        service.openVault(vaultPath, "password".toCharArray());

        assertFalse(service.exists("/toDelete.txt"));
    }
}
