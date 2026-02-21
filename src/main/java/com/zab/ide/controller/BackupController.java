package com.zab.ide.controller;

import com.zab.ide.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
public class BackupController {

    @Autowired
    private BackupService backupService;

    /**
     * Backup webapp
     * POST /api/backup/webapp
     */
    @PostMapping("/webapp")
    public ResponseEntity<?> backupWebapp() {
        try {
            Map<String, Object> result = backupService.backupWebapp();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to backup webapp: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Backup database
     * POST /api/backup/database
     */
    @PostMapping("/database")
    public ResponseEntity<?> backupDatabase() {
        try {
            Map<String, Object> result = backupService.backupDatabase();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to backup database: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get database backup progress
     * GET /api/backup/database/progress
     */
    @GetMapping("/database/progress")
    public ResponseEntity<?> getDatabaseProgress() {
        try {
            Map<String, Object> progress = backupService.getProgress();
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to get progress: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * List all backups
     * GET /api/backup/list
     */
    @GetMapping("/list")
    public ResponseEntity<?> listBackups() {
        try {
            List<Map<String, Object>> backups = backupService.listBackups();
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("backups", backups);
            response.put("count", backups.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to list backups: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Download backup file
     * GET /api/backup/download?filename=xxx.zip
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBackup(@RequestParam String filename) {
        try {
            File file = backupService.getBackupFile(filename);
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentLength(file.length())
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete backup file
     * DELETE /api/backup/delete?filename=xxx.zip
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteBackup(@RequestParam String filename) {
        try {
            boolean deleted = backupService.deleteBackup(filename);
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", deleted);
            response.put("message", deleted ? "Backup deleted" : "Failed to delete backup");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("success", false);
            error.put("message", "Failed to delete backup: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}