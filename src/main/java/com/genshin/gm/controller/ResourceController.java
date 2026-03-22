package com.genshin.gm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 资源控制器
 * - 服务端自行计算 data/txt/ 和 data/bg/ 下所有文件的MD5，存储到 data/version.txt
 * - 客户端通过 GET /api/resource/version 获取 version.txt 内容
 * - 客户端对比本地MD5，不同则从服务端下载对应资源
 */
@RestController
@RequestMapping("/api/resource")
@CrossOrigin(originPatterns = "*")
public class ResourceController {

    private static final Logger logger = LoggerFactory.getLogger(ResourceController.class);
    private static final String DATA_DIR = "data";
    private static final String TXT_DIR = DATA_DIR + "/txt";
    private static final String BG_DIR = DATA_DIR + "/bg";
    private static final String VERSION_FILE = DATA_DIR + "/version.txt";

    /**
     * 启动时计算并写入 version.txt
     */
    @PostConstruct
    public void init() {
        refreshVersionFile();
    }

    /**
     * 获取安卓客户端下载配置（从config.json读取）
     */
    @GetMapping("/app-config")
    public ResponseEntity<java.util.Map<String, String>> getAppConfig() {
        var config = ConfigLoader.getConfig().getApp();
        return ResponseEntity.ok(java.util.Map.of(
            "downloadUrl", config.getDownloadUrl(),
            "version", config.getVersion(),
            "minAndroid", config.getMinAndroid()
        ));
    }

    /**
     * 获取 version.txt（所有资源文件的MD5清单）
     * 每次请求时检查 data/txt/ 和 data/bg/ 是否有文件变动，有则自动刷新
     * 格式: 每行 "路径:md5"，如 "txt/Item.txt:abc123" 或 "bg/bg1.jpg:def456"
     */
    @GetMapping("/version")
    public ResponseEntity<String> getVersion() {
        // 检查data目录的最后修改时间，如果有变动则重新生成version.txt
        refreshIfDataChanged();

        File versionFile = new File(VERSION_FILE);
        if (!versionFile.exists()) {
            refreshVersionFile();
        }
        try {
            String content = Files.readString(versionFile.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content);
        } catch (IOException e) {
            logger.error("读取version.txt失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private long lastDataModTime = 0;

    /**
     * 检查data目录是否有文件变动，有则刷新version.txt
     */
    private void refreshIfDataChanged() {
        long currentMod = getLatestModTime(new File(TXT_DIR))
                + getLatestModTime(new File(BG_DIR));
        if (currentMod != lastDataModTime) {
            lastDataModTime = currentMod;
            refreshVersionFile();
        }
    }

    private long getLatestModTime(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return 0;
        long latest = dir.lastModified();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    latest = Math.max(latest, f.lastModified());
                }
            }
        }
        return latest;
    }

    /**
     * 手动触发刷新 version.txt（管理用）
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        int count = refreshVersionFile();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "version.txt已刷新",
                "fileCount", count
        ));
    }

    /**
     * 下载 data/txt/ 下的文件
     */
    @GetMapping("/download/txt/{fileName:.+}")
    public ResponseEntity<Resource> downloadTxtFile(@PathVariable String fileName) {
        return serveFile(TXT_DIR, fileName);
    }

    /**
     * 下载 data/bg/ 下的文件
     */
    @GetMapping("/download/bg/{fileName:.+}")
    public ResponseEntity<Resource> downloadBgFile(@PathVariable String fileName) {
        return serveFile(BG_DIR, fileName);
    }

    private ResponseEntity<Resource> serveFile(String dir, String fileName) {
        // 防止路径遍历
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(dir, fileName);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = guessContentType(fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    /**
     * 重新计算所有资源文件MD5并写入 data/version.txt
     * 格式: 每行 "子目录/文件名:md5值"
     * 例如: txt/Item.txt:a1b2c3d4e5f6...
     *       bg/background.jpg:f6e5d4c3b2a1...
     */
    private int refreshVersionFile() {
        List<String> lines = new ArrayList<>();

        // 扫描 data/txt/
        scanDirectory(new File(TXT_DIR), "txt", lines);

        // 扫描 data/bg/
        scanDirectory(new File(BG_DIR), "bg", lines);

        // 按文件名排序保持稳定
        lines.sort(String::compareTo);

        // 写入 version.txt
        try {
            Files.writeString(Path.of(VERSION_FILE),
                    String.join("\n", lines) + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("version.txt已更新: {} 个文件", lines.size());
        } catch (IOException e) {
            logger.error("写入version.txt失败", e);
        }

        return lines.size();
    }

    private void scanDirectory(File dir, String prefix, List<String> lines) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isFile() && !file.getName().startsWith(".")) {
                String md5 = computeMd5(file);
                if (md5 != null) {
                    lines.add(prefix + "/" + file.getName() + ":" + md5);
                }
            }
        }
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
            logger.error("计算MD5失败: {}", file.getName(), e);
            return null;
        }
    }

    private String guessContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".txt")) return "text/plain; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }
}
