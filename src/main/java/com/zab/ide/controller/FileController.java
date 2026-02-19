package com.zab.ide.controller;

import com.zab.ide.service.FileManagementService;
import com.zab.ide.service.FileManagementService.FileNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileManagementService fileService;

    /**
     * List files and folders in a directory
     * GET /file/list?path=path/to/folder
     */
    @GetMapping("/list")
    public ResponseEntity<?> listFiles(@RequestParam(defaultValue = "") String path) {
        try {
            List<FileNode> files = fileService.listFilesAndFolders(path);
            return ResponseEntity.ok(files);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get directory tree recursively
     * GET /file/tree?path=path/to/folder&depth=3
     */
    @GetMapping("/tree")
    public ResponseEntity<?> getTree(
            @RequestParam(defaultValue = "") String path,
            @RequestParam(defaultValue = "3") int depth) {
        try {
            FileNode tree = fileService.getDirectoryTree(path, depth);
            return ResponseEntity.ok(tree);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Read file content
     * GET /file/read?path=path/to/file.txt
     */
    @GetMapping("/read")
    public ResponseEntity<?> readFile(@RequestParam String path) {
        try {
            String content = fileService.readFileContent(path);
            Map<String, String> response = new HashMap<String, String>();
            response.put("path", path);
            response.put("content", content);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Write/override file content
     * POST /file/write
     * Body: { "path": "path/to/file.txt", "content": "file content" }
     */
    @PostMapping("/write")
    public ResponseEntity<?> writeFile(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("path") String path) {
        if (path == null || files == null || files.length == 0) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Missing 'path' or 'content' in request");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            // String path = request.get("path");
            // String content = request.get("content");
            for (MultipartFile file : files) {
                // Path filePath = resolvePath(path + file.getOriginalFilename());
                fileService.writeFileContent(path, file);

            }

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "File written successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Create a new file
     * POST /file/create-file
     * Body: { "path": "path/to/newfile.txt" }
     */
    @PostMapping("/create-file")
    public ResponseEntity<?> createFile(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");

            if (path == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "Missing 'path' in request");
                return ResponseEntity.badRequest().body(error);
            }

            fileService.createFile(path);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "File created successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Create a new folder
     * POST /file/create-folder
     * Body: { "path": "path/to/newfolder" }
     */
    @PostMapping("/create-folder")
    public ResponseEntity<?> createFolder(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");

            if (path == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "Missing 'path' in request");
                return ResponseEntity.badRequest().body(error);
            }

            fileService.createFolder(path);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "Folder created successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete file or folder
     * DELETE /file/delete?path=path/to/file-or-folder
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String path) {
        try {
            fileService.delete(path);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "Deleted successfully");
            response.put("path", path);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Rename/move file or folder
     * POST /file/rename
     * Body: { "oldPath": "path/to/old", "newPath": "path/to/new" }
     */
    @PostMapping("/rename")
    public ResponseEntity<?> rename(@RequestBody Map<String, String> request) {
        try {
            String oldPath = request.get("oldPath");
            String newPath = request.get("newPath");

            if (oldPath == null || newPath == null) {
                Map<String, String> error = new HashMap<String, String>();
                error.put("error", "Missing 'oldPath' or 'newPath' in request");
                return ResponseEntity.badRequest().body(error);
            }

            fileService.rename(oldPath, newPath);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("success", true);
            response.put("message", "Renamed successfully");
            response.put("oldPath", oldPath);
            response.put("newPath", newPath);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if path exists
     * GET /file/exists?path=path/to/check
     */
    @GetMapping("/exists")
    public ResponseEntity<?> exists(@RequestParam String path) {
        try {
            boolean exists = fileService.exists(path);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("path", path);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get file/folder information
     * GET /file/info?path=path/to/file-or-folder
     */
    @GetMapping("/info")
    public ResponseEntity<?> getInfo(@RequestParam String path) {
        try {
            FileNode info = fileService.getFileInfo(path);
            return ResponseEntity.ok(info);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Search for files by name pattern
     * GET /file/search?path=path/to/search&pattern=filename
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(defaultValue = "") String path,
            @RequestParam String pattern) {
        try {
            List<FileNode> results = fileService.searchFiles(path, pattern);
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("results", results);
            response.put("count", results.size());
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        } catch (IOException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Download a single file
     * GET /file/download?path=path/to/file.txt
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        try {
            byte[] fileData = fileService.downloadFile(path);
            String fileName = fileService.getFileName(path);

            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download folder as ZIP file
     * GET /file/download-zip?path=path/to/folder
     */
    @GetMapping("/download-zip")
    public ResponseEntity<Resource> downloadFolderAsZip(@RequestParam String path) {
        try {
            ByteArrayOutputStream baos = fileService.downloadFolderAsZip(path);
            String zipFileName = fileService.getZipFileName(path);

            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + zipFileName + "\"")
                    .contentLength(baos.size())
                    .body(resource);

        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}