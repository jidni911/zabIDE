package com.zab.ide.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zab.ide.model.UploadProgress;
import com.zab.ide.service.FileService;
import com.zab.ide.service.RestartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class RootController {

    @Autowired
    private FileService fileService;

    @Autowired
    private RestartService restartService;

    // @GetMapping("")
    // public String root() {
    // return "index.html";
    // }

    /** List files */
    @GetMapping("/allFiles")
    // @ResponseBody
    public List<String> getFiles() throws IOException {
        return fileService.listFiles();
    }

    /** Download file */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {

        Resource resource = fileService.downloadFile(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /** Upload file */
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        fileService.uploadFile(file);
        return ResponseEntity.ok("Uploaded: " + file.getOriginalFilename());
    }

    @PostMapping("/upload/start")
    // @ResponseBody
    public String startUpload(@RequestParam String fileName,
            @RequestParam long totalSize) {
        return fileService.startUpload(fileName, totalSize);
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<?> uploadChunk(@RequestParam("uploadId") String uploadId,
            @RequestParam("chunk") MultipartFile chunk) throws IOException {
        fileService.uploadChunk(uploadId, chunk);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload/finish")
    public ResponseEntity<?> finishUpload(@RequestParam String uploadId,
            @RequestParam String fileName) throws IOException {
        fileService.finishUpload(uploadId, fileName);
        return ResponseEntity.ok("Completed");
    }

    @GetMapping("/upload/progress/{uploadId}")
    // @ResponseBody
    public UploadProgress getProgress(@PathVariable String uploadId) {
        return fileService.getProgress(uploadId);
    }

    @PostMapping("/upload/cancel")
    public ResponseEntity<?> cancelUpload(@RequestParam String uploadId) throws IOException {
        fileService.cancelUpload(uploadId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/download-range/{filename}")
    public ResponseEntity<Resource> downloadRange(
            @PathVariable String filename,
            @RequestHeader HttpHeaders headers) throws IOException {

        return fileService.downloadRange(filename, headers);
    }

    /**
     * Restart Tomcat by modifying web.xml
     */
    @PostMapping("/api/restart-tomcat")
    public ResponseEntity<?> restartTomcat() {
        Map<String, Object> body = new HashMap<>();

        try {
            // Use the safe method that modifies a comment
            restartService.restartTomcatSafe();

            body.put("success", true);
            body.put("message", "Restart initiated successfully");

            return ResponseEntity.ok(body);
            // return ResponseEntity.ok()
            // .body(Map.of(
            // "success", true,
            // "message", "Restart initiated successfully"));
        } catch (IOException e) {
            body.put("success", false);
            body.put("message", "Restart failed: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
            // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            // .body(Map.of(
            // "success", false,
            // "message", "Restart failed: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify server is running
     */
    @GetMapping("/api/health-check")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "healthy");
        body.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(body);
        // return ResponseEntity.ok()
        // .body(Map.of(
        // "status", "healthy",
        // "timestamp", System.currentTimeMillis()));
    }

}