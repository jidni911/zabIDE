package com.zab.ide.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zab.ide.model.UploadProgress;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final String UPLOAD_DIR = "zabftpupload";
    private static final String COMPLETED_DIR = "completed";

    private final Map<String, UploadProgress> progressMap = new ConcurrentHashMap<>();

    private Path getUploadPath() throws IOException {
        String catalinaHome = System.getProperty("catalina.home");
        Path uploadPath = Paths.get(catalinaHome, UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        return uploadPath;
    }

    /** List files */
    public List<String> listFiles() throws IOException {
        Path completedDir = getUploadPath().resolve(COMPLETED_DIR);
        Files.createDirectories(completedDir);
        System.out.println(completedDir);

        List<File> files = Arrays.asList(completedDir.toFile().listFiles());

        return files.stream().map(File::getName).collect(Collectors.toList());
    }

    /** Upload file (streaming) */
    public void uploadFile(MultipartFile file) throws IOException {
        Path target = getUploadPath().resolve(file.getOriginalFilename());

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Download file as Resource */
    public Resource downloadFile(String folderName) throws IOException {

        Path completedDir = getUploadPath().resolve(COMPLETED_DIR).resolve(folderName).normalize();

        if (!Files.exists(completedDir) || !Files.isDirectory(completedDir)) {
            throw new FileNotFoundException("Folder not found: " + folderName);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(completedDir)) {
            for (Path filePath : stream) {
                Resource resource = new UrlResource(filePath.toUri());

                if (resource.exists() && resource.isReadable()) {
                    return resource;
                }
            }
        }

        throw new FileNotFoundException("No file found in folder: " + folderName);
    }

    public String startUpload(String fileName, long totalBytes) {
        String uploadId = UUID.randomUUID().toString();

        UploadProgress progress = new UploadProgress();
        progress.totalBytes = totalBytes;
        progress.uploadedBytes = 0;
        progress.startTime = System.currentTimeMillis();

        progressMap.put(uploadId, progress);

        return uploadId;
    }

    public void uploadChunk(String uploadId, MultipartFile chunk) throws IOException {

        Path tempDir = getUploadPath().resolve("temp");
        Files.createDirectories(tempDir);

        Path tempFile = tempDir.resolve(uploadId + ".tmp");

        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

            InputStream in = chunk.getInputStream();
            byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                progressMap.get(uploadId).uploadedBytes += read;
            }
        }
    }

    public void finishUpload(String uploadId, String fileName) throws IOException {
        // List<Integer> files =
        // listFiles().stream().map(Integer::parseInt).collect(Collectors.toList());
        // Integer nextFile = files.stream().max(Integer::compare).orElse(0) + 1;

        if (fileName.equals("zabide.war")) {
            System.out.println("updating war file");
            String catalinaHome = System.getProperty("catalina.home");
            Path tempFile = getUploadPath().resolve("temp").resolve(uploadId + ".tmp");
            Path finalFile = Paths.get(catalinaHome, "webapps", "zabide.war");

            Files.createDirectories(finalFile.getParent());
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

            progressMap.remove(uploadId);
        } else {

            Path tempFile = getUploadPath().resolve("temp").resolve(uploadId + ".tmp");
            Path finalFile = getUploadPath().resolve(COMPLETED_DIR).resolve(fileName).resolve(fileName);

            Files.createDirectories(finalFile.getParent());
            Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);

            progressMap.remove(uploadId);
        }

    }

    public UploadProgress getProgress(String uploadId) {
        UploadProgress progress = progressMap.get(uploadId);

        if (progress == null) {
            // Optional: return empty object instead of null
            UploadProgress empty = new UploadProgress();
            empty.totalBytes = 0;
            empty.uploadedBytes = 0;
            empty.startTime = 0;
            return empty;
        }

        return progress;
    }

    public void cancelUpload(String uploadId) throws IOException {
        // Clean up temp file
        Path tempFile = getUploadPath().resolve("temp").resolve(uploadId + ".tmp");
        if (Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        progressMap.remove(uploadId);
    }

    private Path getBasePath() {
        // SAME logic you already use
        String catalinaHome = System.getProperty("catalina.home");
        return Paths.get(catalinaHome, UPLOAD_DIR, COMPLETED_DIR);
    }

    public ResponseEntity<Resource> downloadRange(
            String filename,
            HttpHeaders headers) throws IOException {

        Path filePath = getBasePath()
                .resolve(filename)
                .resolve(filename)
                .normalize();

        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        long fileLength = Files.size(filePath);

        // No Range → full file
        if (headers.getRange().isEmpty()) {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(fileLength)
                    .body(resource);
        }

        // Handle Range
        HttpRange range = headers.getRange().get(0);
        long start = range.getRangeStart(fileLength);
        long end = range.getRangeEnd(fileLength);
        long contentLength = end - start + 1;

        // Read the specific range
        byte[] data = new byte[(int) contentLength];
        try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
            file.seek(start);
            file.readFully(data);
        }

        Resource resource = new ByteArrayResource(data);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + fileLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(contentLength)
                .body(resource);
    }

}
