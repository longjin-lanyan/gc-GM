package com.genshin.gm.controller;

import com.genshin.gm.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 公告控制器
 * GET  /api/announcement        — 公开获取当前公告（login 页调用，无需鉴权）
 * POST /api/announcement/update — 管理员更新公告（需 adminToken）
 */
@RestController
@RequestMapping("/api/announcement")
@CrossOrigin(originPatterns = "*")
public class AnnouncementController {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);
    private static final Path ANNOUNCEMENT_FILE = Paths.get("data", "announcement.json");

    // ── 工具：读取存储文件 ────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> readData() {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("title",   "📢 服务器公告");
        def.put("content", "欢迎来到原神私服！");
        def.put("updatedAt", "");
        def.put("enabled", true);

        if (!Files.exists(ANNOUNCEMENT_FILE)) return def;
        try {
            String json = new String(Files.readAllBytes(ANNOUNCEMENT_FILE), StandardCharsets.UTF_8);
            // 简单手写解析，避免引入额外依赖（项目已有 jackson）
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> data = mapper.readValue(json, LinkedHashMap.class);
            return data;
        } catch (Exception e) {
            logger.error("读取公告文件失败", e);
            return def;
        }
    }

    private void writeData(Map<String, Object> data) throws Exception {
        Files.createDirectories(ANNOUNCEMENT_FILE.getParent());
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
        Files.write(ANNOUNCEMENT_FILE,
            mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data));
    }

    private boolean isAdmin(HttpServletRequest request, String bodyToken) {
        String configToken = ConfigLoader.getConfig().getGrasscutter().getAdminToken();
        if (configToken == null || configToken.isEmpty()) return false;
        String token = bodyToken;
        if (token == null) token = request.getHeader("X-Admin-Token");
        if (token == null) token = request.getParameter("adminToken");
        return configToken.trim().equals(token != null ? token.trim() : null);
    }

    // ── GET /api/announcement ────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAnnouncement() {
        Map<String, Object> data = readData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("title",     data.getOrDefault("title",   "📢 服务器公告"));
        result.put("content",   data.getOrDefault("content", "欢迎来到原神私服！"));
        result.put("updatedAt", data.getOrDefault("updatedAt", ""));
        result.put("enabled",   data.getOrDefault("enabled",  true));
        return ResponseEntity.ok(result);
    }

    // ── POST /api/announcement/update ─────────────────────────────────────
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        String adminToken = (String) body.get("adminToken");

        if (!isAdmin(request, adminToken)) {
            result.put("success", false);
            result.put("message", "无权限：adminToken 不正确");
            return ResponseEntity.ok(result);
        }

        String title   = (String) body.getOrDefault("title",   "📢 服务器公告");
        String content = (String) body.getOrDefault("content", "");
        boolean enabled = !Boolean.FALSE.equals(body.getOrDefault("enabled", true));

        if (content == null) content = "";
        if (title == null || title.trim().isEmpty()) title = "📢 服务器公告";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title",     title.trim());
        data.put("content",   content.trim());
        data.put("enabled",   enabled);
        data.put("updatedAt", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        try {
            writeData(data);
            logger.info("公告已更新");
            result.put("success", true);
            result.put("message", "公告已保存");
        } catch (Exception e) {
            logger.error("保存公告失败", e);
            result.put("success", false);
            result.put("message", "保存失败：" + e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
