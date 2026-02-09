package com.genshin.gm.controller;

import com.genshin.gm.service.VerificationService;
import com.genshin.gm.util.SecurityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 验证码控制器
 * 处理验证码的发送和验证
 */
@RestController
@RequestMapping("/api/verification")
@CrossOrigin(originPatterns = "*")
public class VerificationController {
    private static final Logger logger = LoggerFactory.getLogger(VerificationController.class);

    @Autowired
    private VerificationService verificationService;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 发送验证码到游戏内邮箱
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestBody Map<String, String> request,
                                                         HttpServletRequest httpRequest) {
        String uid = request.get("uid");

        if (uid == null || uid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "UID不能为空"
            ));
        }

        String clientIp = getClientIp(httpRequest);
        logger.info("收到发送验证码请求: UID={}, IP={}", uid, clientIp);
        SecurityLogger.logAction(clientIp, null, uid, "SEND_CODE", "请求发送验证码");

        Map<String, Object> result = verificationService.sendVerificationCode(uid);
        return ResponseEntity.ok(result);
    }

    /**
     * 验证验证码
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(@RequestBody Map<String, String> request,
                                                           HttpServletRequest httpRequest) {
        String uid = request.get("uid");
        String code = request.get("code");

        if (uid == null || uid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "UID不能为空"
            ));
        }

        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "验证码不能为空"
            ));
        }

        String clientIp = getClientIp(httpRequest);
        logger.info("收到验证请求: UID={}, Code={}, IP={}", uid, code, clientIp);

        Map<String, Object> result = verificationService.verifyCode(uid, code);

        Boolean success = (Boolean) result.get("success");
        if (Boolean.TRUE.equals(success)) {
            SecurityLogger.logAction(clientIp, null, uid, "VERIFY_SUCCESS", "验证码验证成功");
        } else {
            SecurityLogger.logAction(clientIp, null, uid, "VERIFY_FAIL",
                    "验证码验证失败: " + result.get("message"));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取验证状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(@RequestParam String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "verified", false,
                "message", "UID不能为空"
            ));
        }

        Map<String, Object> status = verificationService.getVerificationStatus(uid);
        return ResponseEntity.ok(status);
    }
}
