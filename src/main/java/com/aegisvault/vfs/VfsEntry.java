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

import java.util.UUID;

public class VfsEntry {

    private final String id;
    private final String name;
    private final boolean directory;
    private final String parentId;
    private long size;
    private long createdAt;
    private long modifiedAt;

    public VfsEntry(String name, boolean directory, String parentId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.directory = directory;
        this.parentId = parentId;
        this.size = 0;
        this.createdAt = System.currentTimeMillis();
        this.modifiedAt = this.createdAt;
    }

    VfsEntry(String id, String name, boolean directory, String parentId,
             long size, long createdAt, long modifiedAt) {
        this.id = id;
        this.name = name;
        this.directory = directory;
        this.parentId = parentId;
        this.size = size;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public boolean isFile() {
        return !directory;
    }

    public String getParentId() {
        return parentId;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
        this.modifiedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void touch() {
        this.modifiedAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return String.format("VfsEntry{id='%s', name='%s', directory=%s, size=%d}",
                id, name, directory, size);
    }
}
