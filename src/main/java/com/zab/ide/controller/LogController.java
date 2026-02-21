package com.zab.ide.controller;

import com.zab.ide.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    /**
     * Get list of log files
     * GET /api/logs/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> listLogFiles() {
        try {
            List<Map<String, Object>> files = logService.getLogFiles();
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("files", files);
            response.put("count", files.size());
            response.put("logsDirectory", logService.getLogsDirectory());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to list log files: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Read log file content
     * GET /api/logs/read?filename=catalina.out
     */
    @GetMapping("/read")
    public ResponseEntity<?> readLogFile(@RequestParam String filename) {
        try {
            String content = logService.readLogFile(filename);
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("content", content);
            response.put("filename", filename);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to read log file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete log file
     * DELETE /api/logs/delete?filename=catalina.2024-02-21.log
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteLogFile(@RequestParam String filename) {
        try {
            boolean deleted = logService.deleteLogFile(filename);
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", deleted);
            response.put("message", deleted ? "Log file deleted" : "Failed to delete log file");
            response.put("filename", filename);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to delete log file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Download log file
     * GET /api/logs/download?filename=catalina.out
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadLogFile(@RequestParam String filename) {
        try {
            File file = logService.getLogFile(filename);
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                    .contentLength(file.length())
                    .body(resource);
                    
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get logs directory info
     * GET /api/logs/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getLogsInfo() {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", true);
        response.put("logsDirectory", logService.getLogsDirectory());
        
        return ResponseEntity.ok(response);
    }
}