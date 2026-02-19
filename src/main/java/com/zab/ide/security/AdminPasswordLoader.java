package com.zab.ide.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AdminPasswordLoader {

    private static final String DEFAULT_PASSWORD = "12345";
    private static final String UPLOAD_DIR = "upload";
    private static final String PASSWORD_FILE = "zabadmin";

    public static String loadPassword() {

        try {
            String catalinaHome = System.getProperty("catalina.home");
            if (catalinaHome == null) {
                throw new IllegalStateException("CATALINA_HOME is not set");
            }

            Path uploadDir = Paths.get(catalinaHome, UPLOAD_DIR);
            Path passwordFile = uploadDir.resolve(PASSWORD_FILE);

            // Create upload directory if missing
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Create password file with default password if missing
            if (!Files.exists(passwordFile)) {
                Files.write(passwordFile, DEFAULT_PASSWORD.getBytes("UTF-8"));
                return DEFAULT_PASSWORD;
            }

            // Read password from file
            return new String(Files.readAllBytes(passwordFile), "UTF-8").trim();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load admin password", e);
        }
    }
}
