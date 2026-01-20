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
package com.aegisvault.service;

import com.aegisvault.exception.AuthenticationException;
import com.aegisvault.vfs.VfsEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VaultServiceTest {

    @TempDir
    Path tempDir;

    private VaultService service;
    private Path vaultPath;

    @BeforeEach
    void setUp() {
        service = new VaultService();
        vaultPath = tempDir.resolve("service-test.avj");
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.close();
        }
    }

    @Test
    void createVaultOpensIt() {
        service.createVault(vaultPath, "password".toCharArray());

        assertTrue(service.isVaultOpen());
        assertEquals(vaultPath, service.getCurrentVaultPath());
    }

    @Test
    void openVaultSucceedsWithCorrectPassword() {
        service.createVault(vaultPath, "password".toCharArray());
        service.close();

        service.openVault(vaultPath, "password".toCharArray());

        assertTrue(service.isVaultOpen());
    }

    @Test
    void openVaultFailsWithWrongPassword() {
        service.createVault(vaultPath, "password".toCharArray());
        service.close();

        assertThrows(AuthenticationException.class,
            () -> service.openVault(vaultPath, "wrong".toCharArray()));
    }

    @Test
    void closeVaultMarksItClosed() {
        service.createVault(vaultPath, "password".toCharArray());

        service.close();

        assertFalse(service.isVaultOpen());
        assertNull(service.getCurrentVaultPath());
    }

    @Test
    void operationsOnClosedVaultThrow() {
        assertThrows(IllegalStateException.class, () -> service.listDirectory("/"));
        assertThrows(IllegalStateException.class, () -> service.createDirectory("/test"));
        assertThrows(IllegalStateException.class, () -> service.createFile("/test", new byte[0]));
    }

    @Test
    void createAndOpenAnotherVaultWithoutCloseThrows() {
        service.createVault(vaultPath, "password".toCharArray());

        Path another = tempDir.resolve("another.avj");
        assertThrows(IllegalStateException.class,
            () -> service.createVault(another, "password".toCharArray()));
    }

    @Test
    void fullWorkflow() {
        service.createVault(vaultPath, "password".toCharArray());

        service.createDirectory("/documents");
        byte[] content = "Report content".getBytes(StandardCharsets.UTF_8);
        service.createFile("/documents/report.txt", content);

        List<VfsEntry> docs = service.listDirectory("/documents");
        assertEquals(1, docs.size());
        assertEquals("report.txt", docs.get(0).getName());

        byte[] read = service.readFile("/documents/report.txt");
        assertArrayEquals(content, read);

        service.close();

        service.openVault(vaultPath, "password".toCharArray());
        assertTrue(service.exists("/documents"));
        assertTrue(service.exists("/documents/report.txt"));
        assertArrayEquals(content, service.readFile("/documents/report.txt"));
    }

    @Test
    void changePasswordWorks() {
        service.createVault(vaultPath, "oldpassword".toCharArray());
        service.createFile("/secret.txt", "data".getBytes(StandardCharsets.UTF_8));

        service.changePassword("oldpassword".toCharArray(), "newpassword".toCharArray());
        service.close();

        service.openVault(vaultPath, "newpassword".toCharArray());
        assertTrue(service.exists("/secret.txt"));
    }

    @Test
    void moveOperationWorks() {
        service.createVault(vaultPath, "password".toCharArray());
        service.createDirectory("/source");
        service.createFile("/source/file.txt", "content".getBytes(StandardCharsets.UTF_8));
        service.createDirectory("/target");

        service.move("/source/file.txt", "/target/moved.txt");

        assertFalse(service.exists("/source/file.txt"));
        assertTrue(service.exists("/target/moved.txt"));
    }

    @Test
    void deleteOperationWorks() {
        service.createVault(vaultPath, "password".toCharArray());
        service.createDirectory("/todelete");
        service.createFile("/todelete/file.txt", "data".getBytes(StandardCharsets.UTF_8));

        service.delete("/todelete");

        assertFalse(service.exists("/todelete"));
    }

    @Test
    void getEntryReturnsCorrectInfo() {
        service.createVault(vaultPath, "password".toCharArray());
        service.createDirectory("/mydir");

        VfsEntry entry = service.getEntry("/mydir");

        assertNotNull(entry);
        assertEquals("mydir", entry.getName());
        assertTrue(entry.isDirectory());
    }
}
