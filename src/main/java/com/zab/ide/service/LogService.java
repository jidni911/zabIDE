package com.zab.ide.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogService {

    private static final String LOGS_DIRECTORY;

    static {
        String catalinaHome = System.getProperty("catalina.home");
        if (catalinaHome == null) {
            catalinaHome = System.getProperty("user.dir");
        }
        LOGS_DIRECTORY = catalinaHome + File.separator + "logs";
    }

    /**
     * Get list of log files sorted by last modified date (desc)
     */
    public List<Map<String, Object>> getLogFiles() throws IOException {
        List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
        File logsDir = new File(LOGS_DIRECTORY);

        if (!logsDir.exists() || !logsDir.isDirectory()) {
            throw new IOException("Logs directory not found: " + LOGS_DIRECTORY);
        }

        File[] logFiles = logsDir.listFiles();
        if (logFiles == null) {
            return files;
        }

        // Sort by last modified date (descending)
        List<File> fileList = new ArrayList<File>();
        for (File file : logFiles) {
            if (file.isFile()) {
                fileList.add(file);
            }
        }

        fileList.sort(new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        // Build file info
        for (File file : fileList) {
            Map<String, Object> fileInfo = new HashMap<String, Object>();
            fileInfo.put("name", file.getName());
            fileInfo.put("size", file.length());
            fileInfo.put("lastModified", file.lastModified());
            fileInfo.put("path", file.getAbsolutePath());
            files.add(fileInfo);
        }

        return files;
    }

    /**
     * Read log file content
     */
    public String readLogFile(String filename) throws IOException {
        validateFilename(filename);

        Path filePath = Paths.get(LOGS_DIRECTORY, filename);
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IOException("Log file not found: " + filename);
        }

        if (!file.isFile()) {
            throw new IOException("Not a file: " + filename);
        }

        // Check file size - limit to 10MB for web display
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.length() > maxSize) {
            // Read last 10MB
            return readLastBytes(file, maxSize);
        }

        // Read entire file
        byte[] bytes = Files.readAllBytes(filePath);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Read last N bytes of a file
     */
    private String readLastBytes(File file, long maxBytes) throws IOException {
        long fileSize = file.length();
        long skipBytes = fileSize - maxBytes;

        byte[] buffer = new byte[(int) maxBytes];
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
        try {
            raf.seek(skipBytes);
            raf.readFully(buffer);
        } finally {
            raf.close();
        }

        return new String(buffer, StandardCharsets.UTF_8);
    }

    /**
     * Delete log file
     */
    public boolean deleteLogFile(String filename) throws IOException {
        validateFilename(filename);

        Path filePath = Paths.get(LOGS_DIRECTORY, filename);
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IOException("Log file not found: " + filename);
        }

        if (!file.isFile()) {
            throw new IOException("Not a file: " + filename);
        }

        return file.delete();
    }

    /**
     * Download log file (returns file path for streaming)
     */
    public File getLogFile(String filename) throws IOException {
        validateFilename(filename);

        Path filePath = Paths.get(LOGS_DIRECTORY, filename);
        File file = filePath.toFile();

        if (!file.exists()) {
            throw new IOException("Log file not found: " + filename);
        }

        if (!file.isFile()) {
            throw new IOException("Not a file: " + filename);
        }

        return file;
    }

    /**
     * Validate filename to prevent path traversal
     */
    private void validateFilename(String filename) throws IOException {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("Filename cannot be empty");
        }

        // Check for path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IOException("Invalid filename");
        }

        // Check for valid log file extensions
        String lowerFilename = filename.toLowerCase();
        if (!lowerFilename.endsWith(".log") && 
            !lowerFilename.endsWith(".txt") && 
            !lowerFilename.endsWith(".out") &&
            !lowerFilename.contains("catalina") &&
            !lowerFilename.contains("localhost")) {
            throw new IOException("Invalid file type");
        }
    }

    /**
     * Get logs directory path
     */
    public String getLogsDirectory() {
        return LOGS_DIRECTORY;
    }
}