package com.genshin.gm.controller;

import com.genshin.gm.service.DeleteAccountService;
import com.genshin.gm.service.GrasscutterService;
import com.genshin.gm.service.UserService;
import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 账号注销控制器
 *
 * POST /api/account/send-delete-code  — 向游戏内发送注销验证码
 * POST /api/account/delete            — 验证代码后执行注销
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin(originPatterns = "*")
public class DeleteAccountController {

    private static final Logger logger = LoggerFactory.getLogger(DeleteAccountController.class);

    /** 验证码有效期（毫秒）5 分钟 */
    private static final long CODE_TTL_MS = 5 * 60 * 1000L;

    /** 内存中存放注销验证码 Map<sessionToken, CodeEntry> */
    private final Map<String, CodeEntry> pendingCodes = new ConcurrentHashMap<>();

    @Autowired private DeleteAccountService deleteAccountService;
    @Autowired private GrasscutterService    grasscutterService;
    @Autowired private UserService           userService;

    // ── 内部类 ─────────────────────────────────────────────────────────────
    private static class CodeEntry {
        final int     code;
        final long    expiresAt;
        final boolean deleteBackend;
        final boolean deleteGame;
        CodeEntry(int code, boolean db, boolean dg) {
            this.code          = code;
            this.expiresAt     = System.currentTimeMillis() + CODE_TTL_MS;
            this.deleteBackend = db;
            this.deleteGame    = dg;
        }
        boolean expired() { return System.currentTimeMillis() > expiresAt; }
    }

    // ── 工具 ───────────────────────────────────────────────────────────────
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }

    // ── Step 1：发送游戏内验证码 ────────────────────────────────────────────
    /**
     * POST /api/account/send-delete-code
     * Body: { "sessionToken": "...", "uid": "12345678",
     *         "deleteBackend": true, "deleteGame": true }
     *
     * 服务器向游戏角色 uid 发送一条包含 4 位随机码的游戏内消息，
     * 玩家需要在游戏中接收后输入该码完成二次验证。
     */
    @PostMapping("/send-delete-code")
    public ResponseEntity<Map<String, Object>> sendDeleteCode(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        String sessionToken = (String) body.get("sessionToken");
        String uid          = (String) body.get("uid");
        boolean deleteBackend = !Boolean.FALSE.equals(body.getOrDefault("deleteBackend", true));
        boolean deleteGame    = !Boolean.FALSE.equals(body.getOrDefault("deleteGame",    true));

        // 验证登录态
        String username = userService.validateSession(sessionToken);
        if (username == null) {
            result.put("success", false);
            result.put("message", "未登录或 session 已过期，请重新登录");
            return ResponseEntity.ok(result);
        }

        if (uid == null || uid.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "请提供游戏 UID");
            return ResponseEntity.ok(result);
        }

        // 生成 4 位随机验证码
        int code = 1000 + new Random().nextInt(9000);

        // 通过 GrasscutterService 向游戏内发送消息
        String serverUrl    = ConfigLoader.getConfig().getGrasscutter().getFullUrl();
        String consoleToken = ConfigLoader.getConfig().getGrasscutter().getConsoleToken();

        String command = "say " + uid.trim() + " 【账号注销验证码】" + code
                + " — 有效期5分钟，请勿告知他人";

        OpenCommandResponse resp = grasscutterService.executeConsoleCommand(
                serverUrl, consoleToken, command, getClientIp(request), username, uid);

        boolean sent = resp != null && (resp.getRetcode() == 0 || resp.isSuccess());
        if (resp != null && !sent) {
            String msg = resp.getMessage() != null ? resp.getMessage() : "";
            // 部分 GC 版本 say 命令也算成功但 retcode 非 0
            if (msg.toLowerCase().contains("sent") || msg.toLowerCase().contains("success")
                    || msg.toLowerCase().contains("成功")) {
                sent = true;
            }
        }

        if (!sent) {
            result.put("success", false);
            result.put("message", "验证码发送失败，请确认游戏服务器在线且 UID 正确");
            return ResponseEntity.ok(result);
        }

        // 存入内存（key = sessionToken，避免不同用户冲突）
        pendingCodes.put(sessionToken, new CodeEntry(code, deleteBackend, deleteGame));
        logger.info("注销验证码已发送 - 用户: {}, UID: {}, code: {}", username, uid, code);

        result.put("success", true);
        result.put("message", "验证码已发送到游戏内角色消息，5 分钟内有效");
        return ResponseEntity.ok(result);
    }

    // ── Step 2：验证代码并执行注销 ──────────────────────────────────────────
    /**
     * POST /api/account/delete
     * Body: { "sessionToken": "...", "code": "1234" }
     *   或（绕过验证，仅当 deleteGame=false 且 deleteBackend=false 时不应发生）
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        Map<String, Object> result = new LinkedHashMap<>();
        String clientIp    = getClientIp(request);
        String sessionToken = (String) body.get("sessionToken");
        String codeStr      = (String) body.get("code");

        // 验证登录态
        String username = userService.validateSession(sessionToken);
        if (username == null) {
            result.put("success", false);
            result.put("message", "未登录或 session 已过期，请重新登录");
            return ResponseEntity.ok(result);
        }

        // 取出待验证记录
        CodeEntry entry = pendingCodes.get(sessionToken);
        if (entry == null) {
            result.put("success", false);
            result.put("message", "请先点击【获取验证码】，完成游戏内验证后再提交");
            return ResponseEntity.ok(result);
        }
        if (entry.expired()) {
            pendingCodes.remove(sessionToken);
            result.put("success", false);
            result.put("message", "验证码已过期，请重新获取");
            return ResponseEntity.ok(result);
        }

        // 校验验证码
        int inputCode;
        try {
            inputCode = Integer.parseInt(codeStr != null ? codeStr.trim() : "");
        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "请输入 4 位数字验证码");
            return ResponseEntity.ok(result);
        }

        if (inputCode != entry.code) {
            result.put("success", false);
            result.put("message", "验证码错误，请检查游戏内消息");
            return ResponseEntity.ok(result);
        }

        // 验证通过，清除记录
        pendingCodes.remove(sessionToken);
        logger.info("注销验证通过 - 用户: {}, IP: {}", username, clientIp);

        // 执行注销
        Map<String, Object> deleteResult = deleteAccountService.deleteAccount(
                sessionToken, entry.deleteBackend, entry.deleteGame, clientIp);
        return ResponseEntity.ok(deleteResult);
    }
}
