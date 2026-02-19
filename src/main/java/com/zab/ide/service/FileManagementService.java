package com.zab.ide.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileManagementService {

    /**
     * Get the base path for file operations (webapps/zab/)
     */
    private Path getBasePath() {
        String catalinaHome = System.getProperty("catalina.home");
        return Paths.get(catalinaHome, "webapps", "zab");
    }

    /**
     * Validate and resolve path to prevent directory traversal attacks
     */
    private Path resolvePath(String relativePath) throws IOException {
        Path basePath = getBasePath();
        Path resolvedPath = basePath.resolve(relativePath).normalize();

        // Security check: ensure the resolved path is still within base path
        if (!resolvedPath.startsWith(basePath)) {
            throw new SecurityException("Access denied: Path traversal attempt detected");
        }

        return resolvedPath;
    }

    /**
     * List files and folders in a directory
     * Returns a tree structure with files and folders
     */
    public List<FileNode> listFilesAndFolders(String relativePath) throws IOException {
        Path targetPath = resolvePath(relativePath);

        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }

        if (!Files.isDirectory(targetPath)) {
            throw new IOException("Path is not a directory: " + relativePath);
        }

        List<FileNode> nodes = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath)) {
            for (Path entry : stream) {
                FileNode node = new FileNode();
                node.name = entry.getFileName().toString();
                node.path = getBasePath().relativize(entry).toString().replace("\\", "/");
                node.isDirectory = Files.isDirectory(entry);
                node.size = Files.isDirectory(entry) ? 0 : Files.size(entry);
                node.lastModified = Files.getLastModifiedTime(entry).toMillis();

                // Get file extension
                if (!node.isDirectory) {
                    String fileName = node.name;
                    int lastDot = fileName.lastIndexOf('.');
                    node.extension = lastDot > 0 ? fileName.substring(lastDot + 1) : "";
                }

                nodes.add(node);
            }
        }

        // Sort: directories first, then files, alphabetically
        nodes.sort((a, b) -> {
            if (a.isDirectory && !b.isDirectory)
                return -1;
            if (!a.isDirectory && b.isDirectory)
                return 1;
            return a.name.compareToIgnoreCase(b.name);
        });

        return nodes;
    }

    /**
     * Get full directory tree recursively
     */
    public FileNode getDirectoryTree(String relativePath, int maxDepth) throws IOException {
        Path targetPath = resolvePath(relativePath);

        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }

        return buildTree(targetPath, 0, maxDepth);
    }

    private FileNode buildTree(Path path, int currentDepth, int maxDepth) throws IOException {
        FileNode node = new FileNode();
        node.name = path.getFileName() != null ? path.getFileName().toString() : "zab";
        node.path = getBasePath().relativize(path).toString().replace("\\", "/");
        node.isDirectory = Files.isDirectory(path);
        node.size = Files.isDirectory(path) ? 0 : Files.size(path);
        node.lastModified = Files.getLastModifiedTime(path).toMillis();

        if (!node.isDirectory) {
            String fileName = node.name;
            int lastDot = fileName.lastIndexOf('.');
            node.extension = lastDot > 0 ? fileName.substring(lastDot + 1) : "";
        }

        if (node.isDirectory && currentDepth < maxDepth) {
            node.children = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    node.children.add(buildTree(entry, currentDepth + 1, maxDepth));
                }
            }

            // Sort children
            node.children.sort((a, b) -> {
                if (a.isDirectory && !b.isDirectory)
                    return -1;
                if (!a.isDirectory && b.isDirectory)
                    return 1;
                return a.name.compareToIgnoreCase(b.name);
            });
        }

        return node;
    }

    /**
     * Read file content as text
     */
    public String readFileContent(String relativePath) throws IOException {
        Path filePath = resolvePath(relativePath);

        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + relativePath);
        }

        if (Files.isDirectory(filePath)) {
            throw new IOException("Cannot read directory as file: " + relativePath);
        }

        // Check file size (prevent reading very large files)
        long fileSize = Files.size(filePath);
        if (fileSize > 10 * 1024 * 1024) { // 10MB limit
            throw new IOException("File too large to read (max 10MB): " + relativePath);
        }

        // Java 8 compatible: read all bytes and convert to string
        byte[] bytes = Files.readAllBytes(filePath);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Write/override file content
     */
    public void writeFileContent(String path, MultipartFile file) throws IOException {
        // Path filePath = resolvePath(relativePath);
        Path filePath = resolvePath(path + file.getOriginalFilename());

        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Files.write(filePath, file.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Create a new file
     */
    public void createFile(String relativePath) throws IOException {
        Path filePath = resolvePath(relativePath);

        if (Files.exists(filePath)) {
            throw new IOException("File already exists: " + relativePath);
        }

        // Create parent directories if they don't exist
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // Create empty file
        Files.createFile(filePath);
    }

    /**
     * Create a new folder
     */
    public void createFolder(String relativePath) throws IOException {
        Path folderPath = resolvePath(relativePath);

        if (Files.exists(folderPath)) {
            throw new IOException("Folder already exists: " + relativePath);
        }

        Files.createDirectories(folderPath);
    }

    /**
     * Delete file or folder
     */
    public void delete(String relativePath) throws IOException {
        Path targetPath = resolvePath(relativePath);

        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }

        if (Files.isDirectory(targetPath)) {
            // Delete directory recursively
            deleteDirectoryRecursively(targetPath);
        } else {
            // Delete file
            Files.delete(targetPath);
        }
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Rename/move file or folder
     */
    public void rename(String oldRelativePath, String newRelativePath) throws IOException {
        Path oldPath = resolvePath(oldRelativePath);
        Path newPath = resolvePath(newRelativePath);

        if (!Files.exists(oldPath)) {
            throw new IOException("Source path does not exist: " + oldRelativePath);
        }

        if (Files.exists(newPath)) {
            throw new IOException("Destination path already exists: " + newRelativePath);
        }

        Files.move(oldPath, newPath);
    }

    /**
     * Check if path exists
     */
    public boolean exists(String relativePath) throws IOException {
        Path targetPath = resolvePath(relativePath);
        return Files.exists(targetPath);
    }

    /**
     * Get file/folder information
     */
    public FileNode getFileInfo(String relativePath) throws IOException {
        Path targetPath = resolvePath(relativePath);

        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }

        FileNode node = new FileNode();
        node.name = targetPath.getFileName().toString();
        node.path = relativePath;
        node.isDirectory = Files.isDirectory(targetPath);
        node.size = Files.isDirectory(targetPath) ? 0 : Files.size(targetPath);
        node.lastModified = Files.getLastModifiedTime(targetPath).toMillis();

        if (!node.isDirectory) {
            String fileName = node.name;
            int lastDot = fileName.lastIndexOf('.');
            node.extension = lastDot > 0 ? fileName.substring(lastDot + 1) : "";
        }

        return node;
    }

    /**
     * Search for files by name pattern
     */
    public List<FileNode> searchFiles(String relativePath, String pattern) throws IOException {
        Path targetPath = resolvePath(relativePath);

        if (!Files.exists(targetPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }

        if (!Files.isDirectory(targetPath)) {
            throw new IOException("Path is not a directory: " + relativePath);
        }

        List<FileNode> results = new ArrayList<>();
        String lowerPattern = pattern.toLowerCase();

        try (Stream<Path> paths = Files.walk(targetPath, 10)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerPattern))
                    .forEach(p -> {
                        try {
                            FileNode node = new FileNode();
                            node.name = p.getFileName().toString();
                            node.path = getBasePath().relativize(p).toString().replace("\\", "/");
                            node.isDirectory = false;
                            node.size = Files.size(p);
                            node.lastModified = Files.getLastModifiedTime(p).toMillis();

                            String fileName = node.name;
                            int lastDot = fileName.lastIndexOf('.');
                            node.extension = lastDot > 0 ? fileName.substring(lastDot + 1) : "";

                            results.add(node);
                        } catch (IOException e) {
                            // Skip files that can't be read
                        }
                    });
        }

        return results;
    }

    /**
     * Inner class representing a file or folder node
     */
    public static class FileNode {
        public String name;
        public String path;
        public boolean isDirectory;
        public long size;
        public long lastModified;
        public String extension;
        public List<FileNode> children;

        public FileNode() {
        }
    }

    /**
     * Download a single file
     */
    public byte[] downloadFile(String relativePath) throws IOException {
        Path filePath = resolvePath(relativePath);

        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + relativePath);
        }

        if (Files.isDirectory(filePath)) {
            throw new IOException("Cannot download directory as file. Use download-zip endpoint.");
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * Get file name from path
     */
    public String getFileName(String relativePath) throws IOException {
        Path filePath = resolvePath(relativePath);
        if (!Files.exists(filePath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }
        return filePath.getFileName().toString();
    }

    /**
     * Download folder as ZIP file
     */
    public ByteArrayOutputStream downloadFolderAsZip(String relativePath) throws IOException {
        Path folderPath = resolvePath(relativePath);

        if (!Files.exists(folderPath)) {
            throw new IOException("Folder does not exist: " + relativePath);
        }

        if (!Files.isDirectory(folderPath)) {
            throw new IOException("Path is not a directory: " + relativePath);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zipFolder(folderPath, folderPath.getFileName().toString(), zos);
        }

        return baos;
    }

    /**
     * Recursively add files to ZIP
     */
    private void zipFolder(Path sourcePath, String parentPath, ZipOutputStream zos) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourcePath)) {
            for (Path entry : stream) {
                String zipEntryName = parentPath + "/" + entry.getFileName().toString();

                if (Files.isDirectory(entry)) {
                    // Add directory entry
                    zipEntryName += "/";
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);
                    zos.closeEntry();

                    // Recurse into directory
                    zipFolder(entry, parentPath + "/" + entry.getFileName().toString(), zos);
                } else {
                    // Add file entry
                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zipEntry.setTime(Files.getLastModifiedTime(entry).toMillis());
                    zos.putNextEntry(zipEntry);

                    // Write file content
                    Files.copy(entry, zos);
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * Get ZIP filename for a folder
     */
    public String getZipFileName(String relativePath) throws IOException {
        Path folderPath = resolvePath(relativePath);
        if (!Files.exists(folderPath)) {
            throw new IOException("Path does not exist: " + relativePath);
        }

        String folderName = folderPath.getFileName().toString();
        return folderName + ".zip";
    }
}