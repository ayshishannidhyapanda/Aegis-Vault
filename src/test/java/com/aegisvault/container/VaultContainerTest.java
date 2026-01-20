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
package com.aegisvault.container;

import com.aegisvault.exception.AuthenticationException;
import com.aegisvault.exception.VaultException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class VaultContainerTest {

    @TempDir
    Path tempDir;

    private Path vaultPath;
    private VaultContainer container;

    @BeforeEach
    void setUp() {
        vaultPath = tempDir.resolve("test.avj");
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.close();
        }
    }

    @Test
    void createVaultCreatesFile() {
        container = new VaultContainer(vaultPath);
        container.create("password123".toCharArray());

        assertTrue(Files.exists(vaultPath));
        assertTrue(container.isOpen());
    }

    @Test
    void createVaultRejectsExistingFile() throws Exception {
        Files.createFile(vaultPath);
        container = new VaultContainer(vaultPath);

        assertThrows(VaultException.class, () -> container.create("password".toCharArray()));
    }

    @Test
    void createVaultRejectsNullPassword() {
        container = new VaultContainer(vaultPath);

        assertThrows(IllegalArgumentException.class, () -> container.create(null));
    }

    @Test
    void createVaultRejectsEmptyPassword() {
        container = new VaultContainer(vaultPath);

        assertThrows(IllegalArgumentException.class, () -> container.create(new char[0]));
    }

    @Test
    void openVaultSucceedsWithCorrectPassword() {
        container = new VaultContainer(vaultPath);
        container.create("password123".toCharArray());
        container.close();

        container = new VaultContainer(vaultPath);
        container.open("password123".toCharArray());

        assertTrue(container.isOpen());
    }

    @Test
    void openVaultFailsWithWrongPassword() {
        container = new VaultContainer(vaultPath);
        container.create("password123".toCharArray());
        container.close();

        container = new VaultContainer(vaultPath);

        assertThrows(AuthenticationException.class, () -> container.open("wrongpassword".toCharArray()));
    }

    @Test
    void openVaultRejectsNonExistentFile() {
        container = new VaultContainer(vaultPath);

        assertThrows(VaultException.class, () -> container.open("password".toCharArray()));
    }

    @Test
    void writeAndReadFileRoundTrip() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());

        byte[] content = "Hello, Vault!".getBytes(StandardCharsets.UTF_8);
        container.writeFile("file1", content);

        byte[] retrieved = container.readFile("file1");
        assertArrayEquals(content, retrieved);
    }

    @Test
    void readFileReturnsNullForNonExistent() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());

        assertNull(container.readFile("nonexistent"));
    }

    @Test
    void deleteFileRemovesFile() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());

        byte[] content = "Data".getBytes(StandardCharsets.UTF_8);
        container.writeFile("file1", content);
        container.deleteFile("file1");

        assertNull(container.readFile("file1"));
    }

    @Test
    void dataPersistedAcrossClose() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());

        byte[] content = "Persisted data".getBytes(StandardCharsets.UTF_8);
        container.writeFile("persistent", content);
        container.close();

        container = new VaultContainer(vaultPath);
        container.open("password".toCharArray());

        byte[] retrieved = container.readFile("persistent");
        assertArrayEquals(content, retrieved);
    }

    @Test
    void closeMarksVaultClosed() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());

        assertTrue(container.isOpen());

        container.close();

        assertFalse(container.isOpen());
    }

    @Test
    void operationsOnClosedVaultThrow() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());
        container.close();

        assertThrows(IllegalStateException.class, () -> container.readFile("file"));
        assertThrows(IllegalStateException.class, () -> container.writeFile("file", new byte[0]));
        assertThrows(IllegalStateException.class, () -> container.deleteFile("file"));
    }

    @Test
    void changePasswordAllowsNewPasswordAccess() {
        container = new VaultContainer(vaultPath);
        container.create("oldpassword".toCharArray());

        byte[] content = "Secret".getBytes(StandardCharsets.UTF_8);
        container.writeFile("file", content);

        container.changePassword("oldpassword".toCharArray(), "newpassword".toCharArray());
        container.close();

        container = new VaultContainer(vaultPath);
        container.open("newpassword".toCharArray());

        byte[] retrieved = container.readFile("file");
        assertArrayEquals(content, retrieved);
    }

    @Test
    void changePasswordInvalidatesOldPassword() {
        container = new VaultContainer(vaultPath);
        container.create("oldpassword".toCharArray());
        container.changePassword("oldpassword".toCharArray(), "newpassword".toCharArray());
        container.close();

        container = new VaultContainer(vaultPath);
        assertThrows(AuthenticationException.class, () -> container.open("oldpassword".toCharArray()));
    }

    @Test
    void multipleFilesSupported() {
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());

        byte[] content1 = "File 1".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "File 2".getBytes(StandardCharsets.UTF_8);
        byte[] content3 = "File 3".getBytes(StandardCharsets.UTF_8);

        container.writeFile("file1", content1);
        container.writeFile("file2", content2);
        container.writeFile("file3", content3);

        assertArrayEquals(content1, container.readFile("file1"));
        assertArrayEquals(content2, container.readFile("file2"));
        assertArrayEquals(content3, container.readFile("file3"));
    }
}
