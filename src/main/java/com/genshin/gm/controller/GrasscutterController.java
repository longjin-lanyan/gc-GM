package com.genshin.gm.controller;

import com.genshin.gm.config.AppConfig;
import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.service.GrasscutterService;
import com.genshin.gm.util.SecurityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 割草机控制器 - 处理前端与Grasscutter服务器的交互
 */
@RestController
@RequestMapping("/api/grasscutter")
@CrossOrigin(originPatterns = "*")
public class GrasscutterController {
    private static final Logger logger = LoggerFactory.getLogger(GrasscutterController.class);

    @Autowired
    private GrasscutterService grasscutterService;

    /**
     * 从HttpServletRequest中获取客户端真实IP
     */
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
     * 验证管理员Token
     * @return null表示验证通过，否则返回错误响应
     */
    private OpenCommandResponse validateAdminToken(String adminToken, HttpServletRequest request) {
        String configAdminToken = ConfigLoader.getConfig().getGrasscutter().getAdminToken();

        if (configAdminToken == null || configAdminToken.isEmpty()
                || "CHANGE_ME_TO_A_SECURE_RANDOM_STRING".equals(configAdminToken)) {
            logger.error("adminToken未配置或使用默认值，拒绝所有管理请求");
            SecurityLogger.logAction(getClientIp(request), null, null, "ADMIN_REJECTED",
                    "adminToken未配置，拒绝管理请求");
            OpenCommandResponse response = new OpenCommandResponse();
            response.setRetcode(403);
            response.setMessage("管理员Token未配置，请在config.json中设置adminToken");
            return response;
        }

        if (adminToken == null || adminToken.trim().isEmpty()) {
            String clientIp = getClientIp(request);
            SecurityLogger.logAction(clientIp, null, null, "ADMIN_NO_TOKEN",
                    "未提供adminToken的管理请求");
            OpenCommandResponse response = new OpenCommandResponse();
            response.setRetcode(401);
            response.setMessage("需要管理员认证，请提供adminToken");
            return response;
        }

        if (!configAdminToken.equals(adminToken.trim())) {
            String clientIp = getClientIp(request);
            SecurityLogger.logAction(clientIp, null, null, "ADMIN_BAD_TOKEN",
                    "adminToken验证失败");
            logger.warn("管理员认证失败 - IP: {}", clientIp);
            OpenCommandResponse response = new OpenCommandResponse();
            response.setRetcode(403);
            response.setMessage("管理员认证失败");
            return response;
        }

        return null; // 验证通过
    }

    /**
     * 获取配置信息（不包含敏感信息）
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        AppConfig.GrasscutterConfig config = ConfigLoader.getConfig().getGrasscutter();
        Map<String, Object> result = new HashMap<>();
        result.put("serverUrl", config.getServerUrl());
        result.put("apiPath", config.getApiPath());
        result.put("fullUrl", config.getFullUrl());
        result.put("hasConsoleToken", !config.getConsoleToken().isEmpty());
        return result;
    }

    /**
     * 测试连接
     */
    @PostMapping("/ping")
    public OpenCommandResponse ping(@RequestBody Map<String, String> request) {
        String serverUrl = request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        logger.info("测试连接到: {}", serverUrl);
        return grasscutterService.ping(serverUrl);
    }

    /**
     * 获取在线玩家
     */
    @PostMapping("/online")
    public OpenCommandResponse getOnlinePlayers(@RequestBody Map<String, String> request) {
        String serverUrl = request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        logger.info("获取在线玩家: {}", serverUrl);
        return grasscutterService.getOnlinePlayers(serverUrl);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/sendCode")
    public OpenCommandResponse sendCode(@RequestBody Map<String, Object> request) {
        String serverUrl = (String) request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        int uid = Integer.parseInt(request.get("uid").toString());
        logger.info("发送验证码到玩家 {}", uid);
        return grasscutterService.sendCode(serverUrl, uid);
    }

    /**
     * 验证验证码
     */
    @PostMapping("/verify")
    public OpenCommandResponse verifyCode(@RequestBody Map<String, Object> request) {
        String serverUrl = (String) request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        String token = (String) request.get("token");
        int code = Integer.parseInt(request.get("code").toString());
        logger.info("验证验证码");
        return grasscutterService.verifyCode(serverUrl, token, code);
    }

    /**
     * 执行命令（玩家模式）
     */
    @PostMapping("/command")
    public OpenCommandResponse executeCommand(@RequestBody Map<String, String> request) {
        String serverUrl = request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        String token = request.get("token");
        String command = request.get("command");
        logger.info("执行命令: {}", command);
        return grasscutterService.executeCommand(serverUrl, token, command);
    }

    /**
     * 执行命令（控制台模式）- 需要管理员认证
     */
    @PostMapping("/console")
    public OpenCommandResponse executeConsoleCommand(@RequestBody Map<String, String> request,
                                                      HttpServletRequest httpRequest) {
        // 验证管理员Token
        String adminToken = request.get("adminToken");
        OpenCommandResponse authError = validateAdminToken(adminToken, httpRequest);
        if (authError != null) {
            return authError;
        }

        String serverUrl = request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        // 使用服务器配置的consoleToken，不允许从请求体覆盖
        String consoleToken = ConfigLoader.getConfig().getGrasscutter().getConsoleToken();
        String command = request.get("command");

        if (consoleToken.isEmpty()) {
            OpenCommandResponse response = new OpenCommandResponse();
            response.setRetcode(400);
            response.setMessage("控制台Token未配置，请在config.json中设置consoleToken");
            return response;
        }

        String clientIp = getClientIp(httpRequest);
        logger.info("管理员执行控制台命令 - IP: {}, 命令: {}", clientIp, command);
        return grasscutterService.executeConsoleCommand(serverUrl, consoleToken, command,
                clientIp, "ADMIN_CONSOLE", null);
    }

    /**
     * 获取运行模式
     */
    @PostMapping("/runmode")
    public OpenCommandResponse getRunMode(@RequestBody Map<String, String> request) {
        String serverUrl = request.getOrDefault("serverUrl",
                ConfigLoader.getConfig().getGrasscutter().getFullUrl());
        String token = request.get("token");
        logger.info("获取运行模式");
        return grasscutterService.getRunMode(serverUrl, token);
    }
}
