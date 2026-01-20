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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VirtualFileSystem {

    private static final String ROOT_ID = "root";
    private static final String VFS_METADATA_KEY = "__vfs_metadata__";

    private final VaultContainer container;
    private final Map<String, VfsEntry> entries;
    private VfsEntry root;

    public VirtualFileSystem(VaultContainer container) {
        if (!container.isOpen()) {
            throw new IllegalArgumentException("Container must be open");
        }
        this.container = container;
        this.entries = new HashMap<>();
        loadOrInitialize();
    }

    private void loadOrInitialize() {
        byte[] metadata = container.readFile(VFS_METADATA_KEY);
        if (metadata != null) {
            deserializeEntries(metadata);
        } else {
            root = new VfsEntry(ROOT_ID, "", true, null, 0, System.currentTimeMillis(), System.currentTimeMillis());
            entries.put(ROOT_ID, root);
            persistMetadata();
        }
    }

    public List<VfsEntry> list(String path) {
        VfsEntry entry = resolvePath(path);
        if (entry == null) {
            throw new VfsException("Path not found: " + path);
        }
        if (!entry.isDirectory()) {
            throw new VfsException("Not a directory: " + path);
        }
        return entries.values().stream()
                .filter(e -> entry.getId().equals(e.getParentId()))
                .collect(Collectors.toList());
    }

    public VfsEntry createDirectory(String path) {
        String[] parts = splitPath(path);
        if (parts.length == 0) {
            throw new VfsException("Invalid path");
        }

        VfsEntry parent = resolveParent(parts);
        String name = parts[parts.length - 1];

        validateName(name);
        checkDuplicate(parent.getId(), name);

        VfsEntry entry = new VfsEntry(name, true, parent.getId());
        entries.put(entry.getId(), entry);
        persistMetadata();
        return entry;
    }

    public VfsEntry createFile(String path, byte[] content) {
        String[] parts = splitPath(path);
        if (parts.length == 0) {
            throw new VfsException("Invalid path");
        }

        VfsEntry parent = resolveParent(parts);
        String name = parts[parts.length - 1];

        validateName(name);
        checkDuplicate(parent.getId(), name);

        VfsEntry entry = new VfsEntry(name, false, parent.getId());
        entry.setSize(content != null ? content.length : 0);
        entries.put(entry.getId(), entry);

        if (content != null && content.length > 0) {
            container.writeFile(entry.getId(), content);
        }

        persistMetadata();
        return entry;
    }

    public byte[] readFile(String path) {
        VfsEntry entry = resolvePath(path);
        if (entry == null) {
            throw new VfsException("File not found: " + path);
        }
        if (entry.isDirectory()) {
            throw new VfsException("Cannot read directory: " + path);
        }
        byte[] data = container.readFile(entry.getId());
        return data != null ? data : new byte[0];
    }

    public void writeFile(String path, byte[] content) {
        VfsEntry entry = resolvePath(path);
        if (entry == null) {
            throw new VfsException("File not found: " + path);
        }
        if (entry.isDirectory()) {
            throw new VfsException("Cannot write to directory: " + path);
        }
        entry.setSize(content != null ? content.length : 0);
        container.writeFile(entry.getId(), content != null ? content : new byte[0]);
        persistMetadata();
    }

    public InputStream openRead(String path) {
        byte[] data = readFile(path);
        return new ByteArrayInputStream(data);
    }

    public OutputStream openWrite(String path) {
        VfsEntry entry = resolvePath(path);
        if (entry == null) {
            throw new VfsException("File not found: " + path);
        }
        if (entry.isDirectory()) {
            throw new VfsException("Cannot write to directory: " + path);
        }
        return new VfsOutputStream(entry);
    }

    public void delete(String path) {
        VfsEntry entry = resolvePath(path);
        if (entry == null) {
            throw new VfsException("Path not found: " + path);
        }
        if (ROOT_ID.equals(entry.getId())) {
            throw new VfsException("Cannot delete root directory");
        }

        deleteRecursive(entry);
        persistMetadata();
    }

    private void deleteRecursive(VfsEntry entry) {
        if (entry.isDirectory()) {
            List<VfsEntry> children = entries.values().stream()
                    .filter(e -> entry.getId().equals(e.getParentId()))
                    .collect(Collectors.toList());
            for (VfsEntry child : children) {
                deleteRecursive(child);
            }
        } else {
            container.deleteFile(entry.getId());
        }
        entries.remove(entry.getId());
    }

    public void move(String sourcePath, String destinationPath) {
        VfsEntry source = resolvePath(sourcePath);
        if (source == null) {
            throw new VfsException("Source not found: " + sourcePath);
        }
        if (ROOT_ID.equals(source.getId())) {
            throw new VfsException("Cannot move root directory");
        }

        String[] destParts = splitPath(destinationPath);
        if (destParts.length == 0) {
            throw new VfsException("Invalid destination path");
        }

        VfsEntry destParent = resolveParent(destParts);
        String newName = destParts[destParts.length - 1];

        validateName(newName);
        checkDuplicate(destParent.getId(), newName);

        VfsEntry moved = new VfsEntry(
                source.getId(),
                newName,
                source.isDirectory(),
                destParent.getId(),
                source.getSize(),
                source.getCreatedAt(),
                System.currentTimeMillis()
        );
        entries.put(moved.getId(), moved);
        persistMetadata();
    }

    public boolean exists(String path) {
        return resolvePath(path) != null;
    }

    public VfsEntry getEntry(String path) {
        return resolvePath(path);
    }

    private VfsEntry resolvePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return entries.get(ROOT_ID);
        }

        String[] parts = splitPath(path);
        VfsEntry current = entries.get(ROOT_ID);

        for (String part : parts) {
            if (current == null || !current.isDirectory()) {
                return null;
            }
            String parentId = current.getId();
            current = entries.values().stream()
                    .filter(e -> parentId.equals(e.getParentId()) && part.equals(e.getName()))
                    .findFirst()
                    .orElse(null);
        }
        return current;
    }

    private VfsEntry resolveParent(String[] parts) {
        if (parts.length == 1) {
            return entries.get(ROOT_ID);
        }
        String[] parentParts = new String[parts.length - 1];
        System.arraycopy(parts, 0, parentParts, 0, parentParts.length);
        VfsEntry parent = resolvePath(String.join("/", parentParts));
        if (parent == null) {
            throw new VfsException("Parent directory not found");
        }
        if (!parent.isDirectory()) {
            throw new VfsException("Parent is not a directory");
        }
        return parent;
    }

    private String[] splitPath(String path) {
        if (path == null || path.isEmpty()) {
            return new String[0];
        }
        String normalized = path.replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return new String[0];
        }
        return normalized.split("/");
    }

    private void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new VfsException("Name cannot be empty");
        }
        if (name.contains("/") || name.contains("\\")) {
            throw new VfsException("Name cannot contain path separators");
        }
        if (".".equals(name) || "..".equals(name)) {
            throw new VfsException("Invalid name: " + name);
        }
    }

    private void checkDuplicate(String parentId, String name) {
        boolean exists = entries.values().stream()
                .anyMatch(e -> parentId.equals(e.getParentId()) && name.equals(e.getName()));
        if (exists) {
            throw new VfsException("Entry already exists: " + name);
        }
    }

    private void persistMetadata() {
        byte[] metadata = serializeEntries();
        container.writeFile(VFS_METADATA_KEY, metadata);
    }

    private byte[] serializeEntries() {
        List<VfsEntry> entryList = new ArrayList<>(entries.values());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer countBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        countBuffer.putInt(entryList.size());
        try {
            baos.write(countBuffer.array());
            for (VfsEntry entry : entryList) {
                byte[] idBytes = entry.getId().getBytes(StandardCharsets.UTF_8);
                byte[] nameBytes = entry.getName().getBytes(StandardCharsets.UTF_8);
                byte[] parentBytes = entry.getParentId() != null ?
                        entry.getParentId().getBytes(StandardCharsets.UTF_8) : new byte[0];

                ByteBuffer entryBuffer = ByteBuffer.allocate(4 + idBytes.length + 4 + nameBytes.length +
                        1 + 4 + parentBytes.length + 8 + 8 + 8).order(ByteOrder.BIG_ENDIAN);

                entryBuffer.putInt(idBytes.length);
                entryBuffer.put(idBytes);
                entryBuffer.putInt(nameBytes.length);
                entryBuffer.put(nameBytes);
                entryBuffer.put((byte) (entry.isDirectory() ? 1 : 0));
                entryBuffer.putInt(parentBytes.length);
                entryBuffer.put(parentBytes);
                entryBuffer.putLong(entry.getSize());
                entryBuffer.putLong(entry.getCreatedAt());
                entryBuffer.putLong(entry.getModifiedAt());

                baos.write(entryBuffer.array());
            }
        } catch (Exception e) {
            throw new VfsException("Failed to serialize entries", e);
        }
        return baos.toByteArray();
    }

    private void deserializeEntries(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        int count = buffer.getInt();

        for (int i = 0; i < count; i++) {
            int idLength = buffer.getInt();
            byte[] idBytes = new byte[idLength];
            buffer.get(idBytes);
            String id = new String(idBytes, StandardCharsets.UTF_8);

            int nameLength = buffer.getInt();
            byte[] nameBytes = new byte[nameLength];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);

            boolean isDir = buffer.get() == 1;

            int parentLength = buffer.getInt();
            String parentId = null;
            if (parentLength > 0) {
                byte[] parentBytes = new byte[parentLength];
                buffer.get(parentBytes);
                parentId = new String(parentBytes, StandardCharsets.UTF_8);
            }

            long size = buffer.getLong();
            long createdAt = buffer.getLong();
            long modifiedAt = buffer.getLong();

            VfsEntry entry = new VfsEntry(id, name, isDir, parentId, size, createdAt, modifiedAt);
            entries.put(id, entry);

            if (ROOT_ID.equals(id)) {
                root = entry;
            }
        }
    }

    private class VfsOutputStream extends ByteArrayOutputStream {
        private final VfsEntry entry;

        VfsOutputStream(VfsEntry entry) {
            this.entry = entry;
        }

        @Override
        public void close() {
            byte[] data = toByteArray();
            entry.setSize(data.length);
            container.writeFile(entry.getId(), data);
            persistMetadata();
        }
    }
}
