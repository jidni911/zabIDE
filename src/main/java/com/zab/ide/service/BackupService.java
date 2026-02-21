package com.zab.ide.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {

    private static final String CATALINA_HOME = System.getProperty("catalina.home", "/opt/tomcat");
    private static final String WEBAPP_PATH = CATALINA_HOME + File.separator + "webapps" + File.separator + "zab";
    private static final String BACKUP_DIR = CATALINA_HOME + File.separator + "dbbackups";
    private static final String CONFIG_FILE = WEBAPP_PATH + File.separator + "WEB-INF" + File.separator + "zab.sys";
    private static final int MAX_DB_BACKUPS = 3;

    private Map<String, Object> currentProgress = new HashMap<String, Object>();
    private List<String> progressLog = new ArrayList<String>();

    /**
     * Backup webapp folder as ZIP
     */
    public Map<String, Object> backupWebapp() throws IOException {
        Map<String, Object> result = new HashMap<String, Object>();

        // Ensure backup directory exists
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        // Generate filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = "ZABWebappBackup_" + timestamp + ".zip";
        File zipFile = new File(backupDir, filename);

        // Create ZIP
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            File sourceDir = new File(WEBAPP_PATH);
            zipDirectory(sourceDir, sourceDir.getName(), zos);
        }

        result.put("success", true);
        result.put("filename", filename);
        result.put("path", zipFile.getAbsolutePath());
        result.put("size", zipFile.length());
        result.put("type", "webapp");
        result.put("created", System.currentTimeMillis());

        return result;
    }

    /**
     * Recursively zip directory
     */
    private void zipDirectory(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(parentFolder + "/" + file.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }

                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * Backup database
     */
    public Map<String, Object> backupDatabase() throws Exception {
        currentProgress.clear();
        progressLog.clear();

        Map<String, Object> result = new HashMap<String, Object>();

        updateProgress(10, "Reading configuration...");
        addLog("Reading database configuration from zab.sys");

        // Parse configuration
        Map<String, String> config = parseZabSysConfig();
        String dbName = config.get("dbname");
        String dbUser = config.get("dbuser");
        String dbPassword = config.get("dbpassword");

        if (dbName == null || dbUser == null || dbPassword == null) {
            throw new Exception("Database configuration not found in zab.sys");
        }

        addLog("Database: " + dbName);
        addLog("User: " + dbUser);

        // Ensure backup directory exists
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        updateProgress(20, "Preparing backup...");

        // Generate filename
        String timestamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String backupFilename = dbName + "Backup-" + timestamp + ".bak";
        String backupPath = BACKUP_DIR + File.separator + backupFilename;

        addLog("Backup file: " + backupFilename);

        updateProgress(30, "Executing SQL Server backup...");
        addLog("Starting SQL Server backup process...");

        // Execute SQL Server backup
        executeSqlServerBackup(dbName, dbUser, dbPassword, backupPath);

        updateProgress(80, "Backup complete, cleaning old backups...");
        addLog("Database backup completed successfully");

        // Clean old backups
        cleanOldDatabaseBackups(dbName);
        addLog("Old backups cleaned (keeping latest " + MAX_DB_BACKUPS + ")");

        updateProgress(90, "Compressing backup...");
        
        // Get file size
        File backupFile = new File(backupPath);
        long fileSize = backupFile.length();

        updateProgress(100, "Complete!");
        addLog("Backup process finished");

        result.put("success", true);
        result.put("filename", backupFilename);
        result.put("path", backupPath);
        result.put("size", fileSize);
        result.put("type", "database");
        result.put("database", dbName);
        result.put("created", System.currentTimeMillis());

        return result;
    }

    /**
     * Parse zab.sys configuration file
     */
    private Map<String, String> parseZabSysConfig() throws IOException {
        Map<String, String> config = new HashMap<String, String>();

        File configFile = new File(CONFIG_FILE);
        if (!configFile.exists()) {
            throw new IOException("Configuration file not found: " + CONFIG_FILE);
        }

        List<String> lines = Files.readAllLines(Paths.get(CONFIG_FILE), StandardCharsets.UTF_8);

        for (String line : lines) {
            line = line.trim();

            // Parse dburl to extract database name
            if (line.startsWith("dburl")) {
                String[] parts = line.split("\"");
                if (parts.length >= 2) {
                    String url = parts[1];
                    // Extract database name from JDBC URL
                    // Format: jdbc:sqlserver://localhost;databaseName=ZABDBNAME;...
                    if (url.contains("databaseName=")) {
                        int start = url.indexOf("databaseName=") + 13;
                        int end = url.indexOf(";", start);
                        if (end == -1) {
                            end = url.length();
                        }
                        String dbName = url.substring(start, end);
                        config.put("dbname", dbName);
                    }
                }
            }

            // Parse dbuser
            if (line.startsWith("dbuser")) {
                String[] parts = line.split("\"");
                if (parts.length >= 2) {
                    config.put("dbuser", parts[1]);
                }
            }

            // Parse dbpassword
            if (line.startsWith("dbpassword")) {
                String[] parts = line.split("\"");
                if (parts.length >= 2) {
                    config.put("dbpassword", parts[1]);
                }
            }
        }

        return config;
    }

    /**
     * Execute SQL Server backup using sqlcmd
     */
    private void executeSqlServerBackup(String dbName, String user, String password, String backupPath) throws Exception {
        // Build SQL command
        String sqlCommand = String.format(
            "BACKUP DATABASE [%s] " +
            "TO DISK = N'%s' " +
            "WITH NOFORMAT, NOINIT, " +
            "NAME = N'%s-Full Database Backup', " +
            "SKIP, NOREWIND, NOUNLOAD, COMPRESSION, STATS = 10",
            dbName, backupPath, dbName
        );

        // Determine sqlcmd path
        String sqlcmdPath = "sqlcmd"; // Assumes sqlcmd is in PATH

        // Build command array
        List<String> command = new ArrayList<String>();
        command.add(sqlcmdPath);
        command.add("-S");
        command.add("localhost");
        command.add("-U");
        command.add(user);
        command.add("-P");
        command.add(password);
        command.add("-Q");
        command.add(sqlCommand);

        // Execute command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // Read output
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                addLog(line);
                
                // Try to parse progress from SQL Server output
                if (line.contains("percent")) {
                    try {
                        String[] parts = line.split("percent");
                        if (parts.length > 0) {
                            String percentStr = parts[0].trim().replaceAll("[^0-9]", "");
                            if (!percentStr.isEmpty()) {
                                int percent = Integer.parseInt(percentStr);
                                // Map SQL progress (0-100) to our progress (30-80)
                                int mappedProgress = 30 + (percent * 50 / 100);
                                updateProgress(mappedProgress, "SQL Server backup: " + percent + "%");
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
            }
        }

        // Wait for completion
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new Exception("SQL Server backup failed with exit code: " + exitCode);
        }
    }

    /**
     * Clean old database backups, keep only latest MAX_DB_BACKUPS
     */
    private void cleanOldDatabaseBackups(String dbName) {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) return;

        // Find all backups for this database
        File[] files = backupDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(dbName + "Backup-") && name.endsWith(".bak");
            }
        });

        if (files == null || files.length <= MAX_DB_BACKUPS) return;

        // Sort by last modified (newest first)
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        // Delete old backups
        for (int i = MAX_DB_BACKUPS; i < files.length; i++) {
            files[i].delete();
            addLog("Deleted old backup: " + files[i].getName());
        }
    }

    /**
     * Get list of all backups
     */
    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> backups = new ArrayList<Map<String, Object>>();

        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) return backups;

        File[] files = backupDir.listFiles();
        if (files == null) return backups;

        // Sort by last modified (newest first)
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        for (File file : files) {
            if (file.isFile()) {
                Map<String, Object> backup = new HashMap<String, Object>();
                backup.put("filename", file.getName());
                backup.put("size", file.length());
                backup.put("created", file.lastModified());
                
                // Determine type
                if (file.getName().endsWith(".bak")) {
                    backup.put("type", "database");
                } else if (file.getName().endsWith(".zip")) {
                    backup.put("type", "webapp");
                } else {
                    backup.put("type", "unknown");
                }
                
                backups.add(backup);
            }
        }

        return backups;
    }

    /**
     * Get backup file
     */
    public File getBackupFile(String filename) throws IOException {
        File file = new File(BACKUP_DIR, filename);
        
        if (!file.exists()) {
            throw new IOException("Backup file not found: " + filename);
        }
        
        if (!file.isFile()) {
            throw new IOException("Not a file: " + filename);
        }
        
        // Security check - ensure file is in backup directory
        if (!file.getCanonicalPath().startsWith(new File(BACKUP_DIR).getCanonicalPath())) {
            throw new IOException("Invalid file path");
        }
        
        return file;
    }

    /**
     * Delete backup file
     */
    public boolean deleteBackup(String filename) throws IOException {
        File file = getBackupFile(filename);
        return file.delete();
    }

    /**
     * Get current backup progress
     */
    public Map<String, Object> getProgress() {
        Map<String, Object> progress = new HashMap<String, Object>(currentProgress);
        progress.put("log", new ArrayList<String>(progressLog));
        return progress;
    }

    /**
     * Update progress
     */
    private void updateProgress(int percent, String message) {
        currentProgress.put("progress", percent);
        currentProgress.put("message", message);
    }

    /**
     * Add log message
     */
    private void addLog(String message) {
        progressLog.add(message);
        System.out.println("[BACKUP] " + message);
    }
}