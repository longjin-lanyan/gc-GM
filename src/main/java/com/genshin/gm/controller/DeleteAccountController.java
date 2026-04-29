package com.genshin.gm.controller;

import com.genshin.gm.service.DeleteAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 账号注销控制器
 * POST /api/account/delete
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin(originPatterns = "*")
public class DeleteAccountController {

    private static final Logger logger = LoggerFactory.getLogger(DeleteAccountController.class);

    @Autowired
    private DeleteAccountService deleteAccountService;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    /**
     * POST /api/account/delete
     *
     * 请求体：
     * {
     *   "sessionToken": "xxx",
     *   "deleteBackend": true,   // 是否删除后台账号，默认 true
     *   "deleteGame":    true    // 是否删除游戏账号，默认 true
     * }
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String  clientIp      = getClientIp(request);
        String  sessionToken  = (String) body.get("sessionToken");
        boolean deleteBackend = !Boolean.FALSE.equals(body.getOrDefault("deleteBackend", true));
        boolean deleteGame    = !Boolean.FALSE.equals(body.getOrDefault("deleteGame", true));

        logger.info("收到账号注销请求 - IP: {}, deleteBackend: {}, deleteGame: {}",
                clientIp, deleteBackend, deleteGame);

        Map<String, Object> result = deleteAccountService.deleteAccount(
                sessionToken, deleteBackend, deleteGame, clientIp);
        return ResponseEntity.ok(result);
    }
}
