package com.genshin.gm.controller;

import com.genshin.gm.proto.*;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource Controller - handles data file MD5 checks and downloads
 */
@RestController
@RequestMapping("/api/resource")
@CrossOrigin(originPatterns = "*")
public class ResourceController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);
    private static final String DATA_DIR = "data";

    // Cache of file MD5s, refreshed on demand
    private final Map<String, String> md5Cache = new HashMap<>();
    private long lastCacheTime = 0;
    private static final long CACHE_TTL_MS = 60_000; // 1 minute

    /**
     * Protobuf endpoint: check which resource files need updating
     */
    @PostMapping(value = "/check", consumes = "application/x-protobuf", produces = "application/x-protobuf")
    public ResponseEntity<byte[]> checkResources(@RequestBody byte[] body) {
        try {
            ResourceCheckRequest request = ResourceCheckRequest.parseFrom(body);
            refreshMd5CacheIfNeeded();

            ResourceCheckResponse.Builder responseBuilder = ResourceCheckResponse.newBuilder();

            // Check each file the client has
            for (ResourceFileInfo clientFile : request.getFilesList()) {
                String serverMd5 = md5Cache.get(clientFile.getFileName());
                if (serverMd5 != null && !serverMd5.equals(clientFile.getMd5())) {
                    responseBuilder.addUpdates(ResourceUpdateInfo.newBuilder()
                            .setFileName(clientFile.getFileName())
                            .setServerMd5(serverMd5)
                            .setDownloadUrl("/api/resource/download/" + clientFile.getFileName())
                            .build());
                }
            }

            // Also include files the client doesn't have
            for (Map.Entry<String, String> entry : md5Cache.entrySet()) {
                boolean clientHas = request.getFilesList().stream()
                        .anyMatch(f -> f.getFileName().equals(entry.getKey()));
                if (!clientHas) {
                    responseBuilder.addUpdates(ResourceUpdateInfo.newBuilder()
                            .setFileName(entry.getKey())
                            .setServerMd5(entry.getValue())
                            .setDownloadUrl("/api/resource/download/" + entry.getKey())
                            .build());
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/x-protobuf"))
                    .body(responseBuilder.build().toByteArray());

        } catch (InvalidProtocolBufferException e) {
            logger.error("Invalid protobuf in resource check", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * JSON endpoint: get all resource file MD5s (for simpler clients)
     */
    @GetMapping("/manifest")
    public ResponseEntity<Map<String, String>> getManifest() {
        refreshMd5CacheIfNeeded();
        return ResponseEntity.ok(new HashMap<>(md5Cache));
    }

    /**
     * Download a specific data file
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        // Security: prevent path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(DATA_DIR, fileName);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Download background images
     */
    @GetMapping("/download/bg/{fileName:.+}")
    public ResponseEntity<Resource> downloadBgFile(@PathVariable String fileName) {
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(DATA_DIR + "/bg", fileName);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                ? "image/jpeg" : fileName.endsWith(".png") ? "image/png" : "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private void refreshMd5CacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheTime < CACHE_TTL_MS && !md5Cache.isEmpty()) {
            return;
        }

        md5Cache.clear();
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            logger.warn("Data directory not found: {}", DATA_DIR);
            return;
        }

        File[] files = dataDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile()) {
                String md5 = computeMd5(file);
                if (md5 != null) {
                    md5Cache.put(file.getName(), md5);
                }
            } else if (file.isDirectory() && "bg".equals(file.getName())) {
                // Also index bg directory
                File[] bgFiles = file.listFiles();
                if (bgFiles != null) {
                    for (File bgFile : bgFiles) {
                        if (bgFile.isFile() && !bgFile.getName().startsWith(".")) {
                            String md5 = computeMd5(bgFile);
                            if (md5 != null) {
                                md5Cache.put("bg/" + bgFile.getName(), md5);
                            }
                        }
                    }
                }
            }
        }

        lastCacheTime = now;
        logger.info("Resource MD5 cache refreshed: {} files", md5Cache.size());
    }

    private String computeMd5(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Failed to compute MD5 for: {}", file.getName(), e);
            return null;
        }
    }
}
