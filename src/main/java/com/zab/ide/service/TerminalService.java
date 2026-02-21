package com.zab.ide.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TerminalService {

    private Process currentProcess = null;
    private static final int COMMAND_TIMEOUT_SECONDS = 30;

    /**
     * Execute a shell command
     */
    public Map<String, Object> executeCommand(String command, String workingDirectory) {
        Map<String, Object> result = new HashMap<String, Object>();

        try {
            // Validate working directory
            File workDir = new File(workingDirectory);
            if (!workDir.exists() || !workDir.isDirectory()) {
                workDir = new File(System.getProperty("catalina.home", "/opt/tomcat"));
            }

            // Handle cd command specially
            if (command.trim().startsWith("cd ")) {
                return handleCdCommand(command, workingDirectory);
            }

            // Determine shell based on OS
            String[] shellCmd;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                shellCmd = new String[]{"cmd.exe", "/c", command};
            } else {
                shellCmd = new String[]{"/bin/bash", "-c", command};
            }

            // Execute command
            ProcessBuilder processBuilder = new ProcessBuilder(shellCmd);
            processBuilder.directory(workDir);
            processBuilder.redirectErrorStream(true);

            currentProcess = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for completion with timeout
            boolean completed = currentProcess.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                currentProcess.destroyForcibly();
                result.put("success", false);
                result.put("message", "Command timed out after " + COMMAND_TIMEOUT_SECONDS + " seconds");
                return result;
            }

            int exitCode = currentProcess.exitValue();

            result.put("success", exitCode == 0);
            result.put("output", output.toString());
            result.put("exitCode", exitCode);
            result.put("workingDirectory", workingDirectory);

            if (exitCode != 0) {
                result.put("message", "Command exited with code " + exitCode);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("output", "");
        } finally {
            currentProcess = null;
        }

        return result;
    }

    /**
     * Handle cd command
     */
    private Map<String, Object> handleCdCommand(String command, String currentDirectory) {
        Map<String, Object> result = new HashMap<String, Object>();

        try {
            String path = command.substring(3).trim();

            File newDir;
            if (path.startsWith("/")) {
                // Absolute path
                newDir = new File(path);
            } else if (path.equals("~")) {
                // Home directory
                newDir = new File(System.getProperty("user.home"));
            } else if (path.equals("..")) {
                // Parent directory
                newDir = new File(currentDirectory).getParentFile();
            } else {
                // Relative path
                newDir = new File(currentDirectory, path);
            }

            if (newDir.exists() && newDir.isDirectory()) {
                result.put("success", true);
                result.put("output", "");
                result.put("workingDirectory", newDir.getCanonicalPath());
            } else {
                result.put("success", false);
                result.put("message", "Directory not found: " + path);
                result.put("output", "bash: cd: " + path + ": No such file or directory");
                result.put("workingDirectory", currentDirectory);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("output", "bash: cd: error");
            result.put("workingDirectory", currentDirectory);
        }

        return result;
    }

    /**
     * Kill current running process
     */
    public boolean killProcess() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
            return true;
        }
        return false;
    }

    /**
     * Get current working directory
     */
    public String getCurrentDirectory() {
        return System.getProperty("user.dir");
    }

    /**
     * Check if process is running
     */
    public boolean isProcessRunning() {
        return currentProcess != null && currentProcess.isAlive();
    }
}