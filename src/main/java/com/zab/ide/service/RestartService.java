package com.zab.ide.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class RestartService {

    /**
     * Restarts Tomcat by toggling a space in web.xml
     * This triggers Tomcat's auto-reload mechanism
     */
    public void restartTomcat() throws IOException {
        String catalinaHome = System.getProperty("catalina.home");

        // Path to web.xml
        Path webXmlPath = Paths.get(catalinaHome, "webapps", "zab", "WEB-INF", "web.xml");

        if (!Files.exists(webXmlPath)) {
            throw new IOException("web.xml not found at: " + webXmlPath);
        }

        // Read the current content
        String content = new String(
                Files.readAllBytes(webXmlPath),
                StandardCharsets.UTF_8);

        // Toggle a space at the end of the file
        String modifiedContent;
        if (content.endsWith(" ")) {
            // Remove trailing space
            modifiedContent = content.substring(0, content.length() - 1);
        } else {
            // Add trailing space
            modifiedContent = content + " ";
        }

        // Write back the modified content
        Files.write(
                webXmlPath,
                content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("Tomcat restart initiated by modifying web.xml");
    }

    /**
     * Alternative: Toggle space in a comment within web.xml
     * This is safer as it doesn't modify the XML structure
     */
    public void restartTomcatSafe() throws IOException {
        String catalinaHome = System.getProperty("catalina.home");
        Path webXmlPath = Paths.get(catalinaHome, "webapps", "zab", "WEB-INF", "web.xml");

        if (!Files.exists(webXmlPath)) {
            throw new IOException("web.xml not found at: " + webXmlPath);
        }

        String content = new String(
                Files.readAllBytes(webXmlPath),
                StandardCharsets.UTF_8);

        // Look for a reload marker comment
        String markerStart = "<!-- RELOAD-MARKER:";
        String markerEnd = "-->";

        int startIndex = content.indexOf(markerStart);

        if (startIndex != -1) {
            // Found marker, toggle the content
            int endIndex = content.indexOf(markerEnd, startIndex);
            if (endIndex != -1) {
                String currentMarker = content.substring(startIndex, endIndex + markerEnd.length());
                String newMarker;

                if (currentMarker.contains(" ")) {
                    // Remove space
                    newMarker = currentMarker.replace(" -->", "-->");
                } else {
                    // Add space
                    newMarker = currentMarker.replace("-->", " -->");
                }

                content = content.replace(currentMarker, newMarker);
            }
        } else {
            // No marker found, add one before </web-app>
            int webAppEndIndex = content.lastIndexOf("</web-app>");
            if (webAppEndIndex != -1) {
                String marker = "\n    <!-- RELOAD-MARKER: Tomcat reload trigger -->\n";
                content = content.substring(0, webAppEndIndex) + marker + content.substring(webAppEndIndex);
            } else {
                throw new IOException("Invalid web.xml format - </web-app> not found");
            }
        }

        Files.write(
                webXmlPath,
                content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("Tomcat restart initiated by modifying web.xml marker");
    }

    /**
     * Health check endpoint - always returns true if server is running
     */
    public boolean healthCheck() {
        return true;
    }
}