package com.zab.ide.controller;

import com.zab.ide.service.TerminalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/terminal")
public class TerminalController {

    @Autowired
    private TerminalService terminalService;

    /**
     * Execute a command
     * POST /api/terminal/execute
     * Body: { "command": "ls -la", "workingDirectory": "/path/to/dir" }
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeCommand(@RequestBody Map<String, String> request) {
        try {
            String command = request.get("command");
            String workingDirectory = request.get("workingDirectory");

            if (command == null || command.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<String, Object>();
                error.put("success", false);
                error.put("message", "Command cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }

            if (workingDirectory == null || workingDirectory.trim().isEmpty()) {
                workingDirectory = System.getProperty("catalina.home", "/opt/tomcat");
            }

            Map<String, Object> result = terminalService.executeCommand(command, workingDirectory);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to execute command: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Kill running process
     * POST /api/terminal/kill
     */
    @PostMapping("/kill")
    public ResponseEntity<?> killProcess() {
        try {
            boolean killed = terminalService.killProcess();
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", killed);
            response.put("message", killed ? "Process killed" : "No process running");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to kill process: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get current directory
     * GET /api/terminal/current-directory
     */
    @GetMapping("/current-directory")
    public ResponseEntity<?> getCurrentDirectory() {
        try {
            String directory = terminalService.getCurrentDirectory();
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("directory", directory);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to get directory: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if process is running
     * GET /api/terminal/status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean running = terminalService.isProcessRunning();
        
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("running", running);
        
        return ResponseEntity.ok(response);
    }
}