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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class VaultService implements Closeable {

    private static final long DEFAULT_TIMEOUT_MS = 15 * 60 * 1000;

    private VaultContainer container;
    private VirtualFileSystem vfs;
    private Path currentVaultPath;
    private Timer autoLockTimer;
    private final AtomicLong lastActivityTime = new AtomicLong();
    private long autoLockTimeoutMs = DEFAULT_TIMEOUT_MS;
    private Runnable onAutoLockCallback;

    public void createVault(Path vaultPath, char[] password) {
        if (isVaultOpen()) {
            throw new IllegalStateException("Another vault is already open. Close it first.");
        }

        try {
            container = new VaultContainer(vaultPath);
            container.create(password.clone());
            vfs = new VirtualFileSystem(container);
            currentVaultPath = vaultPath;
            startAutoLockTimer();
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
            startAutoLockTimer();
        } catch (Exception e) {
            close();
            throw e;
        } finally {
            zeroPassword(password);
        }
    }

    @Override
    public void close() {
        stopAutoLockTimer();

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
        touchActivity();
        return vfs.list(path);
    }

    public VfsEntry createDirectory(String path) {
        ensureVaultOpen();
        touchActivity();
        return vfs.createDirectory(path);
    }

    public VfsEntry createFile(String path, byte[] content) {
        ensureVaultOpen();
        touchActivity();
        return vfs.createFile(path, content);
    }

    public byte[] readFile(String path) {
        ensureVaultOpen();
        touchActivity();
        return vfs.readFile(path);
    }

    public void writeFile(String path, byte[] content) {
        ensureVaultOpen();
        touchActivity();
        vfs.writeFile(path, content);
    }

    public void delete(String path) {
        ensureVaultOpen();
        touchActivity();
        vfs.delete(path);
    }

    public void move(String source, String destination) {
        ensureVaultOpen();
        touchActivity();
        vfs.move(source, destination);
    }

    public boolean exists(String path) {
        ensureVaultOpen();
        touchActivity();
        return vfs.exists(path);
    }

    public VfsEntry getEntry(String path) {
        ensureVaultOpen();
        touchActivity();
        return vfs.getEntry(path);
    }

    public void changePassword(char[] currentPassword, char[] newPassword) {
        ensureVaultOpen();
        touchActivity();
        try {
            container.changePassword(currentPassword.clone(), newPassword.clone());
        } finally {
            zeroPassword(currentPassword);
            zeroPassword(newPassword);
        }
    }

    public void setAutoLockTimeout(long timeoutMs) {
        this.autoLockTimeoutMs = timeoutMs;
        if (isVaultOpen()) {
            stopAutoLockTimer();
            startAutoLockTimer();
        }
    }

    public long getAutoLockTimeout() {
        return autoLockTimeoutMs;
    }

    public void setOnAutoLockCallback(Runnable callback) {
        this.onAutoLockCallback = callback;
    }

    public void touchActivity() {
        lastActivityTime.set(System.currentTimeMillis());
    }

    private void startAutoLockTimer() {
        touchActivity();
        autoLockTimer = new Timer("VaultAutoLock", true);
        autoLockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isVaultOpen()) {
                    long elapsed = System.currentTimeMillis() - lastActivityTime.get();
                    if (elapsed >= autoLockTimeoutMs) {
                        close();
                        if (onAutoLockCallback != null) {
                            onAutoLockCallback.run();
                        }
                    }
                }
            }
        }, 60000, 60000);
    }

    private void stopAutoLockTimer() {
        if (autoLockTimer != null) {
            autoLockTimer.cancel();
            autoLockTimer = null;
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
