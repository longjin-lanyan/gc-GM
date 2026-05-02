package com.genshin.gm.controller;

import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.entity.IpAccountRecord;
import com.genshin.gm.repository.IpAccountRecordRepository;
import com.genshin.gm.util.SecurityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * IP 注册配额管理接口（仅管理员可用）
 *
 * GET  /api/admin/ip-quota/list           — 列出所有 IP 的注册情况
 * POST /api/admin/ip-quota/set            — 手动调整某 IP 的计数值
 * POST /api/admin/ip-quota/reset          — 清零某 IP 的所有记录
 * POST /api/admin/ip-quota/delete-record  — 删除某 IP 下的某一条具体记录
 */
@RestController
@RequestMapping("/api/admin/ip-quota")
@CrossOrigin(originPatterns = "*")
public class IpQuotaAdminController {

    private static final Logger logger = LoggerFactory.getLogger(IpQuotaAdminController.class);
    private static final int MAX_PER_IP = 3;

    @Autowired
    private IpAccountRecordRepository ipAccountRecordRepository;

    // ── 鉴权 ──────────────────────────────────────────────────────────────────

    private boolean isAdmin(HttpServletRequest request, String bodyToken) {
        String configToken = ConfigLoader.getConfig().getGrasscutter().getAdminToken();
        if (configToken == null || configToken.isEmpty()) {
            logger.warn("adminToken 未配置，管理员接口已拒绝访问");
            return false;
        }
        // 优先从请求体取，再从 Header 取，最后从 URL 参数取
        String token = bodyToken;
        if (token == null) token = request.getHeader("X-Admin-Token");
        if (token == null) token = request.getParameter("adminToken");
        return configToken.trim().equals(token != null ? token.trim() : null);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    // ── 列出所有 IP 注册情况 ────────────────────────────────────────────────

    /**
     * GET /api/admin/ip-quota/list?adminToken=xxx
     * 返回所有 IP 及其下的账号列表、已用配额、剩余配额
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAll(
            @RequestParam(required = false) String adminToken,
            HttpServletRequest request) {

        if (!isAdmin(request, adminToken)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限"));
        }

        List<IpAccountRecord> all = ipAccountRecordRepository.findAll();

        // 按 IP 分组
        Map<String, List<IpAccountRecord>> byIp = all.stream()
                .collect(Collectors.groupingBy(IpAccountRecord::getIpAddress,
                        LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<IpAccountRecord>> entry : byIp.entrySet()) {
            String ip = entry.getKey();
            List<IpAccountRecord> records = entry.getValue();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ip", ip);
            row.put("used", records.size());
            row.put("max", MAX_PER_IP);
            row.put("remaining", Math.max(0, MAX_PER_IP - records.size()));
            row.put("accounts", records.stream().map(r -> {
                Map<String, Object> acc = new LinkedHashMap<>();
                acc.put("id", r.getId());
                acc.put("username", r.getUsername());
                acc.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                return acc;
            }).collect(Collectors.toList()));
            result.add(row);
        }

        // 按 used 降序排列（最多注册的 IP 排在最前）
        result.sort((a, b) -> Integer.compare((int) b.get("used"), (int) a.get("used")));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("total", result.size());
        resp.put("data", result);
        return ResponseEntity.ok(resp);
    }

    // ── 手动设置某 IP 的配额值 ──────────────────────────────────────────────

    /**
     * POST /api/admin/ip-quota/set
     * Body: { "adminToken": "...", "ip": "1.2.3.4", "count": 1 }
     * 将该 IP 的计数设置为指定值（通过增删 IpAccountRecord 记录来实现）
     */
    @PostMapping("/set")
    public ResponseEntity<Map<String, Object>> setCount(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String adminToken = (String) body.get("adminToken");
        if (!isAdmin(request, adminToken)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限"));
        }

        String ip = (String) body.get("ip");
        Object countObj = body.get("count");

        if (ip == null || ip.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供 IP 地址"));
        }
        if (countObj == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供目标计数值"));
        }

        int targetCount;
        try {
            targetCount = Integer.parseInt(countObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "count 必须为整数"));
        }

        if (targetCount < 0 || targetCount > 99) {
            return ResponseEntity.ok(Map.of("success", false, "message", "count 范围：0 ~ 99"));
        }

        // 查出该 IP 当前记录
        List<IpAccountRecord> records = ipAccountRecordRepository.findAll().stream()
                .filter(r -> ip.trim().equals(r.getIpAddress()))
                .collect(Collectors.toList());

        int current = records.size();
        String adminIp = getClientIp(request);

        if (targetCount < current) {
            // 需要删除若干条记录（从最新的开始删）
            records.sort(Comparator.comparing(IpAccountRecord::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            int toDelete = current - targetCount;
            for (int i = 0; i < toDelete; i++) {
                ipAccountRecordRepository.delete(records.get(i));
            }
            logger.info("管理员 {} 将 IP {} 的配额从 {} 调整为 {}", adminIp, ip, current, targetCount);
            SecurityLogger.logAction(adminIp, "admin", null, "IP_QUOTA_SET",
                    ip + ": " + current + " → " + targetCount);
        } else if (targetCount > current) {
            // 需要插入若干占位记录
            int toAdd = targetCount - current;
            for (int i = 0; i < toAdd; i++) {
                IpAccountRecord placeholder = new IpAccountRecord(ip.trim(), "[admin-placeholder-" + i + "]");
                ipAccountRecordRepository.save(placeholder);
            }
            logger.info("管理员 {} 将 IP {} 的配额从 {} 增加到 {}", adminIp, ip, current, targetCount);
            SecurityLogger.logAction(adminIp, "admin", null, "IP_QUOTA_SET",
                    ip + ": " + current + " → " + targetCount);
        }
        // current == targetCount 时无需操作

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "IP " + ip + " 的配额已更新为 " + targetCount);
        resp.put("ip", ip);
        resp.put("before", current);
        resp.put("after", targetCount);
        return ResponseEntity.ok(resp);
    }

    // ── 清零某 IP 的所有记录 ────────────────────────────────────────────────

    /**
     * POST /api/admin/ip-quota/reset
     * Body: { "adminToken": "...", "ip": "1.2.3.4" }
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetIp(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String adminToken = (String) body.get("adminToken");
        if (!isAdmin(request, adminToken)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限"));
        }

        String ip = (String) body.get("ip");
        if (ip == null || ip.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供 IP 地址"));
        }

        List<IpAccountRecord> records = ipAccountRecordRepository.findAll().stream()
                .filter(r -> ip.trim().equals(r.getIpAddress()))
                .collect(Collectors.toList());

        int deleted = records.size();
        ipAccountRecordRepository.deleteAll(records);

        String adminIp = getClientIp(request);
        logger.info("管理员 {} 清零了 IP {} 的所有 {} 条注册记录", adminIp, ip, deleted);
        SecurityLogger.logAction(adminIp, "admin", null, "IP_QUOTA_RESET",
                ip + ": 清零 " + deleted + " 条记录");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "IP " + ip + " 的注册记录已全部清除（共 " + deleted + " 条）",
                "deletedCount", deleted
        ));
    }

    // ── 删除某条具体记录 ────────────────────────────────────────────────────

    /**
     * POST /api/admin/ip-quota/delete-record
     * Body: { "adminToken": "...", "id": 123 }
     */
    @PostMapping("/delete-record")
    public ResponseEntity<Map<String, Object>> deleteRecord(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String adminToken = (String) body.get("adminToken");
        if (!isAdmin(request, adminToken)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限"));
        }

        Object idObj = body.get("id");
        if (idObj == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "请提供记录 ID"));
        }

        long id;
        try {
            id = Long.parseLong(idObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "ID 必须为整数"));
        }

        if (!ipAccountRecordRepository.existsById(id)) {
            return ResponseEntity.ok(Map.of("success", false, "message", "记录不存在"));
        }

        ipAccountRecordRepository.deleteById(id);
        String adminIp = getClientIp(request);
        logger.info("管理员 {} 删除了 IP 注册记录 ID={}", adminIp, id);
        SecurityLogger.logAction(adminIp, "admin", null, "IP_RECORD_DELETE", "record id=" + id);

        return ResponseEntity.ok(Map.of("success", true, "message", "记录已删除"));
    }

    // ── 清空所有 IP 注册记录 ────────────────────────────────────────────────

    /**
     * POST /api/admin/ip-quota/clear-all
     * Body: { "adminToken": "..." }
     * 删除 ip_account_records 表中的全部记录，所有 IP 配额归零。
     */
    @PostMapping("/clear-all")
    public ResponseEntity<Map<String, Object>> clearAll(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String adminToken = (String) body.get("adminToken");
        if (!isAdmin(request, adminToken)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "无权限"));
        }

        long total = ipAccountRecordRepository.count();
        ipAccountRecordRepository.deleteAll();

        String adminIp = getClientIp(request);
        logger.warn("管理员 {} 执行了一键清空所有IP注册记录，共删除 {} 条", adminIp, total);
        SecurityLogger.logAction(adminIp, "admin", null, "IP_QUOTA_CLEAR_ALL",
                "清空全部 " + total + " 条IP注册记录");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已清空所有 IP 注册记录，共删除 " + total + " 条",
                "deletedCount", total
        ));
    }
}
