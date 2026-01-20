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
import com.aegisvault.vfs.VfsEntry;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public final class ImportExportUtil {

    private ImportExportUtil() {
    }

    public static int importFile(VaultService service, Path source, String targetDir) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("Source must be a regular file");
        }

        byte[] content = Files.readAllBytes(source);
        String targetPath = normalizePath(targetDir, source.getFileName().toString());
        service.createFile(targetPath, content);
        return 1;
    }

    public static int importDirectory(VaultService service, Path source, String targetDir) throws IOException {
        if (!Files.isDirectory(source)) {
            throw new IllegalArgumentException("Source must be a directory");
        }

        int[] count = {0};
        String basePath = normalizePath(targetDir, source.getFileName().toString());
        service.createDirectory(basePath);
        count[0]++;

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(source)) {
                    String relativePath = source.relativize(dir).toString().replace("\\", "/");
                    String vaultPath = normalizePath(basePath, relativePath);
                    service.createDirectory(vaultPath);
                    count[0]++;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = source.relativize(file).toString().replace("\\", "/");
                String vaultPath = normalizePath(basePath, relativePath);
                byte[] content = Files.readAllBytes(file);
                service.createFile(vaultPath, content);
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });

        return count[0];
    }

    public static int exportFile(VaultService service, String sourcePath, Path targetDir) throws IOException {
        VfsEntry entry = service.getEntry(sourcePath);
        if (entry == null) {
            throw new IllegalArgumentException("Source path not found: " + sourcePath);
        }

        if (entry.isFile()) {
            byte[] content = service.readFile(sourcePath);
            Path targetFile = targetDir.resolve(entry.getName());
            Files.write(targetFile, content);
            return 1;
        } else {
            return exportDirectory(service, sourcePath, targetDir);
        }
    }

    public static int exportDirectory(VaultService service, String sourcePath, Path targetDir) throws IOException {
        VfsEntry entry = service.getEntry(sourcePath);
        if (entry == null || !entry.isDirectory()) {
            throw new IllegalArgumentException("Source must be a directory: " + sourcePath);
        }

        int count = 0;
        Path targetPath = targetDir.resolve(entry.getName());
        Files.createDirectories(targetPath);
        count++;

        List<VfsEntry> children = service.listDirectory(sourcePath);
        for (VfsEntry child : children) {
            String childPath = normalizePath(sourcePath, child.getName());
            if (child.isDirectory()) {
                count += exportDirectory(service, childPath, targetPath);
            } else {
                byte[] content = service.readFile(childPath);
                Files.write(targetPath.resolve(child.getName()), content);
                count++;
            }
        }

        return count;
    }

    private static String normalizePath(String base, String name) {
        if (base.equals("/")) {
            return "/" + name;
        }
        return base + "/" + name;
    }
}
