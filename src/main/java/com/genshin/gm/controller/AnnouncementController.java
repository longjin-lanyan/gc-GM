package com.genshin.gm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.genshin.gm.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
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
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    /** 公告文件路径，与 config.json、data/ 同级 */
    private static File getFile() {
        return new File("data" + File.separator + "announcement.json");
    }

    // ── 默认值 ────────────────────────────────────────────────────────────
    private static Map<String, Object> defaultData() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("title",     "📢 服务器公告");
        d.put("content",   "欢迎来到原神私服！");
        d.put("updatedAt", "");
        d.put("enabled",   true);
        return d;
    }

    // ── 读取 ──────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> readData() {
        File f = getFile();
        if (!f.exists()) return defaultData();
        try {
            return MAPPER.readValue(f, LinkedHashMap.class);
        } catch (Exception e) {
            logger.error("读取公告文件失败: {}", e.getMessage());
            return defaultData();
        }
    }

    // ── 写入 ──────────────────────────────────────────────────────────────
    private void writeData(Map<String, Object> data) throws Exception {
        File f = getFile();
        // 确保 data/ 目录存在
        if (!f.getParentFile().exists()) {
            boolean ok = f.getParentFile().mkdirs();
            if (!ok) throw new RuntimeException("无法创建目录: " + f.getParentFile().getAbsolutePath());
        }
        MAPPER.writeValue(f, data);
        logger.info("公告已写入: {}", f.getAbsolutePath());
    }

    // ── Token 验证 ────────────────────────────────────────────────────────
    private boolean isAdmin(HttpServletRequest request, String bodyToken) {
        String configToken;
        try {
            configToken = ConfigLoader.getConfig().getGrasscutter().getAdminToken();
        } catch (Exception e) {
            logger.error("读取 adminToken 失败", e);
            return false;
        }
        if (configToken == null || configToken.isEmpty()) {
            logger.warn("adminToken 未配置");
            return false;
        }
        String token = bodyToken;
        if (token == null || token.isEmpty()) token = request.getHeader("X-Admin-Token");
        if (token == null || token.isEmpty()) token = request.getParameter("adminToken");
        boolean ok = configToken.trim().equals(token != null ? token.trim() : "");
        if (!ok) logger.warn("adminToken 不匹配, 收到: [{}]", token);
        return ok;
    }

    // ── GET /api/announcement ─────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAnnouncement() {
        Map<String, Object> data = readData();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success",   true);
        result.put("title",     data.getOrDefault("title",     "📢 服务器公告"));
        result.put("content",   data.getOrDefault("content",   "欢迎来到原神私服！"));
        result.put("updatedAt", data.getOrDefault("updatedAt", ""));
        result.put("enabled",   data.getOrDefault("enabled",   true));
        return ResponseEntity.ok(result);
    }

    // ── POST /api/announcement/update ─────────────────────────────────────
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateAnnouncement(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        String adminToken = (String) body.get("adminToken");

        logger.info("收到公告更新请求, token=[{}]", adminToken);

        if (!isAdmin(request, adminToken)) {
            result.put("success", false);
            result.put("message", "无权限：adminToken 不正确，请检查 config.json 中的 grasscutter.adminToken");
            return ResponseEntity.ok(result);
        }

        String  title   = (String)  body.getOrDefault("title",   "📢 服务器公告");
        String  content = (String)  body.getOrDefault("content", "");
        boolean enabled = !Boolean.FALSE.equals(body.getOrDefault("enabled", true));

        if (title   == null || title.trim().isEmpty()) title = "📢 服务器公告";
        if (content == null) content = "";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title",     title.trim());
        data.put("content",   content.trim());
        data.put("enabled",   enabled);
        data.put("updatedAt", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        try {
            writeData(data);
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
