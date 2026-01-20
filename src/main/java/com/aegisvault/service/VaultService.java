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

import com.aegisvault.container.VaultContainer;
import com.aegisvault.vfs.VfsEntry;
import com.aegisvault.vfs.VirtualFileSystem;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class VaultService implements Closeable {

    private VaultContainer container;
    private VirtualFileSystem vfs;
    private Path currentVaultPath;

    public void createVault(Path vaultPath, char[] password) {
        if (isVaultOpen()) {
            throw new IllegalStateException("Another vault is already open. Close it first.");
        }

        try {
            container = new VaultContainer(vaultPath);
            container.create(password.clone());
            vfs = new VirtualFileSystem(container);
            currentVaultPath = vaultPath;
        } catch (Exception e) {
            close();
            throw e;
        } finally {
            zeroPassword(password);
        }
    }

    public void openVault(Path vaultPath, char[] password) {
        if (isVaultOpen()) {
            throw new IllegalStateException("Another vault is already open. Close it first.");
        }

        try {
            container = new VaultContainer(vaultPath);
            container.open(password.clone());
            vfs = new VirtualFileSystem(container);
            currentVaultPath = vaultPath;
        } catch (Exception e) {
            close();
            throw e;
        } finally {
            zeroPassword(password);
        }
    }

    @Override
    public void close() {
        if (vfs != null) {
            vfs = null;
        }

        if (container != null) {
            container.close();
            container = null;
        }

        currentVaultPath = null;
    }

    public boolean isVaultOpen() {
        return container != null && container.isOpen();
    }

    public Path getCurrentVaultPath() {
        return currentVaultPath;
    }

    public List<VfsEntry> listDirectory(String path) {
        ensureVaultOpen();
        return vfs.list(path);
    }

    public VfsEntry createDirectory(String path) {
        ensureVaultOpen();
        return vfs.createDirectory(path);
    }

    public VfsEntry createFile(String path, byte[] content) {
        ensureVaultOpen();
        return vfs.createFile(path, content);
    }

    public byte[] readFile(String path) {
        ensureVaultOpen();
        return vfs.readFile(path);
    }

    public void writeFile(String path, byte[] content) {
        ensureVaultOpen();
        vfs.writeFile(path, content);
    }

    public void delete(String path) {
        ensureVaultOpen();
        vfs.delete(path);
    }

    public void move(String source, String destination) {
        ensureVaultOpen();
        vfs.move(source, destination);
    }

    public boolean exists(String path) {
        ensureVaultOpen();
        return vfs.exists(path);
    }

    public VfsEntry getEntry(String path) {
        ensureVaultOpen();
        return vfs.getEntry(path);
    }

    public void changePassword(char[] currentPassword, char[] newPassword) {
        ensureVaultOpen();
        try {
            container.changePassword(currentPassword.clone(), newPassword.clone());
        } finally {
            zeroPassword(currentPassword);
            zeroPassword(newPassword);
        }
    }

    private void ensureVaultOpen() {
        if (!isVaultOpen()) {
            throw new IllegalStateException("No vault is currently open");
        }
    }

    private void zeroPassword(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
