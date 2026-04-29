package com.genshin.gm.controller;

import com.genshin.gm.service.RegisterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 公开注册控制器 - 供玩家自助注册原神私服账号
 * 无需管理员权限，但受IP限制（每IP最多3个账号）
 */
@RestController
@RequestMapping("/api/register")
@CrossOrigin(originPatterns = "*")
public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @Autowired
    private RegisterService registerService;

    /**
     * 从请求中提取真实客户端IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个IP，取第一个（最原始的）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * POST /api/register/account
     * 玩家自助注册游戏账号
     *
     * 请求体: { "username": "账号名" }
     * 响应: { "success": true/false, "message": "...", "username": "...", "remainingQuota": N }
     */
    @PostMapping("/account")
    public ResponseEntity<Map<String, Object>> registerAccount(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String clientIp = getClientIp(request);
        String username = body.get("username");

        logger.info("收到注册请求 - IP: {}, 账号: {}", clientIp, username);

        Map<String, Object> result = registerService.registerGameAccount(username, clientIp);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/register/quota
     * 查询当前IP的注册配额使用情况
     *
     * 响应: { "used": N, "max": 3, "remaining": N, "allowed": true/false }
     */
    @GetMapping("/quota")
    public ResponseEntity<Map<String, Object>> getQuota(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Map<String, Object> result = registerService.getIpQuota(clientIp);
        return ResponseEntity.ok(result);
    }
}
