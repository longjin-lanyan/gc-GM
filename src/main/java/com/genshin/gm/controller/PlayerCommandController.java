package com.genshin.gm.controller;

import com.genshin.gm.config.AppConfig;
import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.model.PlayerCommand;
import com.genshin.gm.service.GrasscutterService;
import com.genshin.gm.service.PlayerCommandService;
import com.genshin.gm.service.UserService;
import com.genshin.gm.service.VerificationService;
import com.genshin.gm.util.CommandProcessor;
import com.genshin.gm.util.SecurityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 玩家指令控制器
 */
@RestController
@RequestMapping("/api/commands")
@CrossOrigin(originPatterns = "*")
public class PlayerCommandController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerCommandController.class);

    @Autowired
    private PlayerCommandService service;

    @Autowired
    private GrasscutterService grasscutterService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private UserService userService;

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
        // X-Forwarded-For 可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 严格验证UID归属权
     * 1. 如果有sessionToken → 验证UID是否绑定到该用户账户
     * 2. 如果没有sessionToken → 检查临时验证状态
     * 3. 如果UID不属于该用户 → 记录到 errohuman.txt
     *
     * @return null 表示验证通过，否则返回错误信息Map
     */
    private Map<String, Object> validateUidOwnership(String sessionToken, String uid,
                                                      String command, HttpServletRequest request) {
        String clientIp = getClientIp(request);

        // 情况1：有sessionToken（已登录用户）
        if (sessionToken != null && !sessionToken.trim().isEmpty()) {
            String username = userService.validateSession(sessionToken);

            if (username == null) {
                // session无效/过期
                SecurityLogger.logAction(clientIp, null, uid, "SESSION_INVALID",
                        "使用无效sessionToken尝试执行指令");
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "登录已过期，请重新登录");
                return err;
            }

            // 检查UID是否绑定到该用户
            if (userService.isUidVerified(username, uid)) {
                // 验证通过
                SecurityLogger.logAction(clientIp, username, uid, "EXECUTE_CMD",
                        "永久绑定验证通过, 指令: " + (command != null ? command : "预设指令"));
                return null;
            }

            // UID未绑定到该用户 → 安全事件！
            String boundUids = userService.getVerifiedUidsString(username);
            SecurityLogger.logUnauthorizedUidAttempt(
                    clientIp, username, boundUids, uid, command,
                    "已登录用户尝试操作非绑定UID，疑似提权攻击"
            );

            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "该UID未绑定到您的账户，无法执行操作");
            return err;
        }

        // 情况2：无sessionToken（未登录用户），使用临时验证
        boolean tempVerified = verificationService.isVerified(uid);
        if (tempVerified) {
            SecurityLogger.logAction(clientIp, null, uid, "EXECUTE_CMD",
                    "临时验证通过, 指令: " + (command != null ? command : "预设指令"));
            return null;
        }

        // 未验证
        SecurityLogger.logAction(clientIp, null, uid, "UNVERIFIED",
                "未验证UID尝试执行指令");
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", "请先验证您的UID");
        err.put("needVerification", true);
        return err;
    }

    /**
     * 提交新指令
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitCommand(@RequestBody PlayerCommand command,
                                                              HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String clientIp = getClientIp(request);
            SecurityLogger.logAction(clientIp, null, null, "SUBMIT_CMD",
                    "提交指令: " + command.getCommand());

            // 验证指令格式
            String validationError = CommandProcessor.validateCommand(command.getCommand());
            if (validationError != null) {
                response.put("success", false);
                response.put("message", validationError);
                return ResponseEntity.ok(response);
            }

            PlayerCommand saved = service.submitCommand(command);
            response.put("success", true);
            response.put("message", "指令提交成功，等待审核");
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("提交指令失败", e);
            response.put("success", false);
            response.put("message", "提交失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取所有已审核通过的指令
     */
    @GetMapping("/approved")
    public ResponseEntity<List<PlayerCommand>> getApprovedCommands(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "time") String sort) {

        try {
            List<PlayerCommand> commands;

            if (category != null && !category.isEmpty()) {
                commands = service.getApprovedCommandsByCategory(category);
            } else if ("popular".equals(sort)) {
                commands = service.getPopularCommands();
            } else {
                commands = service.getApprovedCommands();
            }

            return ResponseEntity.ok(commands);
        } catch (Exception e) {
            logger.error("获取指令列表失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 增加浏览数
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Map<String, Object>> incrementViews(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.incrementViews(id);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("增加浏览数失败", e);
            response.put("success", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 点赞（需要UID）
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likeCommand(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String uid = body.get("uid");
            if (uid == null || uid.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请提供UID");
                return ResponseEntity.ok(response);
            }

            boolean success = service.likeCommand(id, uid);
            if (success) {
                response.put("success", true);
                response.put("message", "点赞成功");
            } else {
                response.put("success", false);
                response.put("message", "您已经点过赞了");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("点赞失败", e);
            response.put("success", false);
            response.put("message", "点赞失败");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 执行指令到玩家账户（通过OpenCommand）
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeCommand(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        try {
            String uid = body.get("uid");
            String sessionToken = body.get("sessionToken");

            if (uid == null || uid.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请提供UID");
                return ResponseEntity.ok(response);
            }

            // 获取指令详情（先获取以便记录日志）
            Optional<PlayerCommand> optional = service.getCommandById(id);
            if (!optional.isPresent()) {
                response.put("success", false);
                response.put("message", "指令不存在");
                return ResponseEntity.ok(response);
            }
            PlayerCommand command = optional.get();

            // 严格验证UID归属权
            Map<String, Object> validationErr = validateUidOwnership(
                    sessionToken, uid, command.getCommand(), request);
            if (validationErr != null) {
                return ResponseEntity.ok(validationErr);
            }

            // 增加浏览数
            service.incrementViews(id);

            // 获取Grasscutter配置
            AppConfig config = ConfigLoader.getConfig();
            if (config == null || config.getGrasscutter() == null) {
                response.put("success", false);
                response.put("message", "Grasscutter配置未找到");
                return ResponseEntity.ok(response);
            }

            AppConfig.GrasscutterConfig gcConfig = config.getGrasscutter();

            // 使用智能处理器处理指令，自动添加UID
            String finalCommand = CommandProcessor.processCommand(command.getCommand(), uid);

            logger.info("执行指令 - UID: {} (已验证), 原始: {}, 处理后: {}", uid, command.getCommand(), finalCommand);

            // 验证通过后，使用控制台token执行指令
            OpenCommandResponse result = grasscutterService.executeConsoleCommand(
                    gcConfig.getFullUrl(),
                    gcConfig.getConsoleToken(),
                    finalCommand
            );

            logger.info("OpenCommand响应 - retcode: {}, message: {}, data: {}",
                    result.getRetcode(), result.getMessage(), result.getData());

            if (result.getRetcode() == 200) {
                String resultData = result.getData() != null ? result.getData().toString() : "";

                if (resultData.contains("用法：") || resultData.contains("此命令需要")) {
                    response.put("success", false);
                    response.put("message", "指令格式错误");
                    response.put("data", resultData);
                    response.put("debug", "处理后的指令: " + finalCommand);
                } else if (resultData.contains("无权限") || resultData.contains("No permission")) {
                    response.put("success", false);
                    response.put("message", "权限不足（这不应该发生，请检查控制台token配置）");
                    response.put("data", resultData);
                } else {
                    response.put("success", true);
                    response.put("message", "指令执行成功");
                    response.put("data", resultData);
                }
            } else {
                response.put("success", false);
                response.put("message", "指令执行失败: " + result.getMessage());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("执行指令失败", e);
            response.put("success", false);
            response.put("message", "执行失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取所有指令（管理后台）
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<PlayerCommand>> getAllCommands() {
        try {
            return ResponseEntity.ok(service.getAllCommands());
        } catch (Exception e) {
            logger.error("获取所有指令失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 获取待审核的指令（管理后台）
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<List<PlayerCommand>> getPendingCommands() {
        try {
            return ResponseEntity.ok(service.getPendingCommands());
        } catch (Exception e) {
            logger.error("获取待审核指令失败", e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 审核通过（管理后台）
     */
    @PostMapping("/admin/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveCommand(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String reviewNote = body != null ? body.get("reviewNote") : "";
            String category = body != null ? body.get("category") : null;
            PlayerCommand updated = service.approveCommand(id, reviewNote, category);

            if (updated != null) {
                response.put("success", true);
                response.put("message", "审核通过");
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "指令不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("审核通过失败", e);
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 审核拒绝（管理后台）
     */
    @PostMapping("/admin/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectCommand(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        try {
            String reviewNote = body != null ? body.get("reviewNote") : "";
            String category = body != null ? body.get("category") : null;
            PlayerCommand updated = service.rejectCommand(id, reviewNote, category);

            if (updated != null) {
                response.put("success", true);
                response.put("message", "已拒绝");
                response.put("data", updated);
            } else {
                response.put("success", false);
                response.put("message", "指令不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("审核拒绝失败", e);
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 删除指令（管理后台）
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Map<String, Object>> deleteCommand(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            service.deleteCommand(id);
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除指令失败", e);
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 执行自定义指令（带安全验证）
     */
    @PostMapping("/custom/execute")
    public ResponseEntity<Map<String, Object>> executeCustomCommand(@RequestBody Map<String, String> body,
                                                                     HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String uid = body.get("uid");
            String command = body.get("command");
            String sessionToken = body.get("sessionToken");

            if (uid == null || uid.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请提供UID");
                return ResponseEntity.ok(response);
            }

            if (command == null || command.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "请输入指令");
                return ResponseEntity.ok(response);
            }

            command = command.trim();

            // 1. 检查是否为危险指令
            String dangerousCheck = CommandProcessor.checkDangerousCommand(command);
            if (dangerousCheck != null) {
                String clientIp = getClientIp(request);
                response.put("success", false);
                response.put("message", dangerousCheck);
                response.put("dangerous", true);
                logger.warn("UID {} 尝试执行危险指令: {}", uid, command);
                SecurityLogger.logAction(clientIp, null, uid, "DANGEROUS_CMD",
                        "尝试执行危险指令: " + command);
                return ResponseEntity.ok(response);
            }

            // 2. 验证指令格式
            String validationError = CommandProcessor.validateCommand(command);
            if (validationError != null) {
                response.put("success", false);
                response.put("message", "指令格式错误: " + validationError);
                return ResponseEntity.ok(response);
            }

            // 3. 严格验证UID归属权
            Map<String, Object> validationErr = validateUidOwnership(sessionToken, uid, command, request);
            if (validationErr != null) {
                return ResponseEntity.ok(validationErr);
            }

            // 4. 处理指令（添加UID）
            String finalCommand = CommandProcessor.processCommand(command, uid);
            logger.info("执行自定义指令: UID={}, 原始={}, 处理后={}", uid, command, finalCommand);

            // 5. 使用控制台token执行指令
            AppConfig.GrasscutterConfig gcConfig = ConfigLoader.getConfig().getGrasscutter();
            OpenCommandResponse result = grasscutterService.executeConsoleCommand(
                    gcConfig.getFullUrl(),
                    gcConfig.getConsoleToken(),
                    finalCommand
            );

            // 6. 处理执行结果
            if (result != null && result.getRetcode() == 200) {
                response.put("success", true);
                String resultData = result.getData() != null ? result.getData().toString() : "指令执行成功";
                response.put("data", resultData);
                response.put("message", "指令执行成功");
                logger.info("自定义指令执行成功: UID={}, 指令={}", uid, finalCommand);
            } else {
                response.put("success", false);
                String errorMsg = result != null ? result.getMessage() : "未知错误";
                response.put("message", "执行失败: " + errorMsg);
                logger.error("自定义指令执行失败: UID={}, 指令={}, 错误={}", uid, finalCommand, errorMsg);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("执行自定义指令失败", e);
            response.put("success", false);
            response.put("message", "执行失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
