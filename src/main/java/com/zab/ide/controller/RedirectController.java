package com.zab.ide.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class RedirectController {

    public static final String URL = "https://oslpay.zaberp.com/zabftp";
    public static final String VERSION = "1.0.1";

    private static final Path UPDATE_LOCK = Paths.get(System.getProperty("catalina.home"), "update.lock");

    private static LocalDateTime LAST_UPDATE_CHECK = LocalDateTime.now();

    /* ================= ROOT ================= */

    @GetMapping("")
    public String redirectRoot() {
        // selfUpdateAsync(); // fire & forget
        return "redirect:/index.html";
    }

    /* ================= VERSION ================= */

    @GetMapping("/version")
    @ResponseBody
    public String getVersion() {
        return VERSION;
    }

    // @GetMapping("/selfDownload")
    // public ResponseEntity<Resource> selfDownload() throws Exception {
    //     String catalinaHome = System.getProperty("catalina.home");
    //     Path warPath = Paths.get(catalinaHome, "webapps", "zabftp.war");

    //     if (!Files.exists(warPath)) {
    //         throw new RuntimeException("WAR file not found!");
    //     }

    //     Resource resource = new org.springframework.core.io.FileSystemResource(warPath.toFile());

    //     return ResponseEntity.ok()
    //             .contentType(MediaType.APPLICATION_OCTET_STREAM)
    //             .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
    //             .body(resource);
    // }

    // @PostMapping("/selfUpload")
    // @ResponseBody
    // public ResponseEntity<String> selfUpload(@RequestParam("file") MultipartFile file) {
    //     if (file.isEmpty()) {
    //         return ResponseEntity.badRequest().body("File is empty");
    //     }

    //     if (!file.getOriginalFilename().endsWith(".war")) {
    //         return ResponseEntity.badRequest().body("Only WAR files are allowed");
    //     }

    //     try {
    //         String catalinaHome = System.getProperty("catalina.home");
    //         Path tempWar = Paths.get(catalinaHome, "zabftp_new.war");

    //         // Save uploaded WAR
    //         Files.copy(file.getInputStream(), tempWar, StandardCopyOption.REPLACE_EXISTING);

    //         // Fire async update
    //         selfUpdateFromLocalAsync(tempWar);

    //         return ResponseEntity.ok("Upload successful. Update started.");

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed");
    //     }
    // }

    // /* ================= ASYNC UPDATE ================= */

    // @Async
    // public void selfUpdateFromLocalAsync(Path newWar) {

    //     // Prevent parallel updates
    //     if (Files.exists(UPDATE_LOCK)) {
    //         System.out.println("Update already in progress");
    //         return;
    //     }

    //     try {
    //         Files.createFile(UPDATE_LOCK);

    //         String catalinaHome = System.getProperty("catalina.home");
    //         Path webapps = Paths.get(catalinaHome, "webapps");
    //         Path activeWar = webapps.resolve("zabftp.war");

    //         System.out.println("Applying self-update...");

    //         // Replace active WAR
    //         Files.deleteIfExists(activeWar);
    //         Files.move(newWar, activeWar, StandardCopyOption.REPLACE_EXISTING);

    //         System.out.println("WAR updated successfully. Tomcat will redeploy.");

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         try {
    //             Files.deleteIfExists(UPDATE_LOCK);
    //         } catch (Exception ignored) {
    //         }
    //     }
    // }

}
