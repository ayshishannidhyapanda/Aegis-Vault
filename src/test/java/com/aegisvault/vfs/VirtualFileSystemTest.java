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
package com.aegisvault.vfs;

import com.aegisvault.container.VaultContainer;
import com.aegisvault.exception.VfsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VirtualFileSystemTest {

    @TempDir
    Path tempDir;

    private VaultContainer container;
    private VirtualFileSystem vfs;

    @BeforeEach
    void setUp() {
        Path vaultPath = tempDir.resolve("test.avj");
        container = new VaultContainer(vaultPath);
        container.create("password".toCharArray());
        vfs = new VirtualFileSystem(container);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.close();
        }
    }

    @Test
    void listRootReturnsEmptyInitially() {
        List<VfsEntry> entries = vfs.list("/");
        assertTrue(entries.isEmpty());
    }

    @Test
    void createDirectorySucceeds() {
        VfsEntry dir = vfs.createDirectory("/docs");

        assertNotNull(dir);
        assertEquals("docs", dir.getName());
        assertTrue(dir.isDirectory());
    }

    @Test
    void createNestedDirectorySucceeds() {
        vfs.createDirectory("/docs");
        VfsEntry nested = vfs.createDirectory("/docs/archive");

        assertNotNull(nested);
        assertEquals("archive", nested.getName());
        assertTrue(nested.isDirectory());
    }

    @Test
    void createFileSucceeds() {
        byte[] content = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        VfsEntry file = vfs.createFile("/readme.txt", content);

        assertNotNull(file);
        assertEquals("readme.txt", file.getName());
        assertTrue(file.isFile());
        assertEquals(content.length, file.getSize());
    }

    @Test
    void readFileReturnsContent() {
        byte[] content = "Test content".getBytes(StandardCharsets.UTF_8);
        vfs.createFile("/test.txt", content);

        byte[] read = vfs.readFile("/test.txt");
        assertArrayEquals(content, read);
    }

    @Test
    void writeFileUpdatesContent() {
        byte[] original = "Original".getBytes(StandardCharsets.UTF_8);
        byte[] updated = "Updated content".getBytes(StandardCharsets.UTF_8);

        vfs.createFile("/file.txt", original);
        vfs.writeFile("/file.txt", updated);

        byte[] read = vfs.readFile("/file.txt");
        assertArrayEquals(updated, read);
    }

    @Test
    void deleteFileRemovesIt() {
        vfs.createFile("/todelete.txt", "data".getBytes(StandardCharsets.UTF_8));

        vfs.delete("/todelete.txt");

        assertFalse(vfs.exists("/todelete.txt"));
    }

    @Test
    void deleteDirectoryRemovesItAndChildren() {
        vfs.createDirectory("/parent");
        vfs.createDirectory("/parent/child");
        vfs.createFile("/parent/file.txt", "data".getBytes(StandardCharsets.UTF_8));

        vfs.delete("/parent");

        assertFalse(vfs.exists("/parent"));
        assertFalse(vfs.exists("/parent/child"));
        assertFalse(vfs.exists("/parent/file.txt"));
    }

    @Test
    void deleteRootThrows() {
        assertThrows(VfsException.class, () -> vfs.delete("/"));
    }

    @Test
    void moveFileSucceeds() {
        vfs.createFile("/original.txt", "content".getBytes(StandardCharsets.UTF_8));
        vfs.createDirectory("/target");

        vfs.move("/original.txt", "/target/moved.txt");

        assertFalse(vfs.exists("/original.txt"));
        assertTrue(vfs.exists("/target/moved.txt"));
    }

    @Test
    void moveDirectorySucceeds() {
        vfs.createDirectory("/source");
        vfs.createFile("/source/file.txt", "data".getBytes(StandardCharsets.UTF_8));

        vfs.move("/source", "/renamed");

        assertFalse(vfs.exists("/source"));
        assertTrue(vfs.exists("/renamed"));
    }

    @Test
    void existsReturnsTrueForExistingPath() {
        vfs.createFile("/exists.txt", new byte[0]);

        assertTrue(vfs.exists("/exists.txt"));
    }

    @Test
    void existsReturnsFalseForNonExistingPath() {
        assertFalse(vfs.exists("/nonexistent.txt"));
    }

    @Test
    void getEntryReturnsCorrectEntry() {
        vfs.createDirectory("/mydir");

        VfsEntry entry = vfs.getEntry("/mydir");

        assertNotNull(entry);
        assertEquals("mydir", entry.getName());
        assertTrue(entry.isDirectory());
    }

    @Test
    void createDuplicateThrows() {
        vfs.createDirectory("/duplicate");

        assertThrows(VfsException.class, () -> vfs.createDirectory("/duplicate"));
    }

    @Test
    void createInNonExistentParentThrows() {
        assertThrows(VfsException.class, () -> vfs.createFile("/nonexistent/file.txt", new byte[0]));
    }

    @Test
    void readDirectoryThrows() {
        vfs.createDirectory("/dir");

        assertThrows(VfsException.class, () -> vfs.readFile("/dir"));
    }

    @Test
    void writeToDirectoryThrows() {
        vfs.createDirectory("/dir");

        assertThrows(VfsException.class, () -> vfs.writeFile("/dir", new byte[0]));
    }

    @Test
    void listNonDirectoryThrows() {
        vfs.createFile("/file.txt", new byte[0]);

        assertThrows(VfsException.class, () -> vfs.list("/file.txt"));
    }

    @Test
    void invalidNamesRejected() {
        assertThrows(VfsException.class, () -> vfs.createDirectory("/."));
        assertThrows(VfsException.class, () -> vfs.createDirectory("/.."));
    }

    @Test
    void dataPersistsAcrossVfsInstances() {
        byte[] content = "Persistent".getBytes(StandardCharsets.UTF_8);
        vfs.createDirectory("/persist");
        vfs.createFile("/persist/data.txt", content);

        VirtualFileSystem vfs2 = new VirtualFileSystem(container);

        assertTrue(vfs2.exists("/persist"));
        assertTrue(vfs2.exists("/persist/data.txt"));
        assertArrayEquals(content, vfs2.readFile("/persist/data.txt"));
    }
}
