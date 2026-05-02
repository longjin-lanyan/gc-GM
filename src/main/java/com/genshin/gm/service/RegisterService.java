package com.genshin.gm.service;

import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.entity.IpAccountRecord;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.repository.IpAccountRecordRepository;
import com.genshin.gm.util.SecurityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 原神私服公开注册服务
 * 通过 gc-opencommand-plugin 的控制台接口执行 "account create <username>" 命令
 */
@Service
public class RegisterService {

    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

    /** 每个IP最多允许创建的账号数量 */
    private static final int MAX_ACCOUNTS_PER_IP = 3;

    /** 用户名规则：3-20位字母数字下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\s\\S]{2,}$");

    @Autowired
    private IpAccountRecordRepository ipAccountRecordRepository;

    @Autowired
    private GrasscutterService grasscutterService;

    /**
     * 注册原神私服账号
     *
     * @param username 要注册的游戏账号名
     * @param clientIp 客户端IP地址
     * @return 包含 success/message 的结果Map
     */
    public Map<String, Object> registerGameAccount(String username, String clientIp) {
        Map<String, Object> result = new HashMap<>();

        // 1. 校验用户名格式
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "账号名不能为空");
            return result;
        }
        username = username.trim();

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            result.put("success", false);
            result.put("message", "账号名至少需要2个字符");
            return result;
        }

        // 2. 全局查重：同一账号名不允许重复注册
        if (ipAccountRecordRepository.existsByUsername(username)) {
            logger.warn("账号名已存在 - IP: {}, 账号: {}", clientIp, username);
            result.put("success", false);
            result.put("message", "账号名 " + username + " 已被注册，请换一个名字");
            return result;
        }

        // 3. 检查IP注册限制
        long ipCount = ipAccountRecordRepository.countByIpAddress(clientIp);
        if (ipCount >= MAX_ACCOUNTS_PER_IP) {
            logger.warn("IP {} 已达到注册上限 ({}/{})", clientIp, ipCount, MAX_ACCOUNTS_PER_IP);
            SecurityLogger.logAction(clientIp, null, null, "REGISTER_IP_LIMIT",
                    "IP已超过注册上限: " + username);
            result.put("success", false);
            result.put("message", "该IP已达到最大注册数量（" + MAX_ACCOUNTS_PER_IP + "个），无法继续注册");
            result.put("currentCount", ipCount);
            result.put("maxCount", MAX_ACCOUNTS_PER_IP);
            return result;
        }

        // 4. 检查账号名是否已在本地记录中（防止重复注册）
        if (ipAccountRecordRepository.existsByUsername(username)) {
            logger.warn("账号名重复注册 - IP: {}, 账号: {}", clientIp, username);
            result.put("success", false);
            result.put("message", "账号名 \"" + username + "\" 已被注册，请换一个名字");
            return result;
        }

        // 5. 构造并执行 account create 命令
        String command = "account create " + username;
        String serverUrl = ConfigLoader.getConfig().getGrasscutter().getFullUrl();
        String consoleToken = ConfigLoader.getConfig().getGrasscutter().getConsoleToken();

        if (consoleToken == null || consoleToken.isEmpty()) {
            logger.error("consoleToken未配置，无法执行账号注册命令");
            result.put("success", false);
            result.put("message", "服务器配置错误，请联系管理员");
            return result;
        }

        logger.info("执行账号注册 - IP: {}, 账号: {}, 命令: {}", clientIp, username, command);

        OpenCommandResponse gcResponse = grasscutterService.executeConsoleCommand(
                serverUrl, consoleToken, command, clientIp, "PUBLIC_REGISTER", username
        );

        // 5. 处理 Grasscutter 返回结果
        if (gcResponse == null) {
            logger.error("Grasscutter返回null - IP: {}, 账号: {}", clientIp, username);
            result.put("success", false);
            result.put("message", "服务器无响应，请稍后重试");
            return result;
        }

        // retcode=0 表示成功；部分版本用 message 判断
        boolean gcSuccess = gcResponse.getRetcode() == 0 || gcResponse.isSuccess();

        // 有时候 retcode 非 0 但 message 包含成功信息（如 "Account created"）
        String gcMessage = gcResponse.getMessage() != null ? gcResponse.getMessage() : "";
        if (!gcSuccess && (gcMessage.toLowerCase().contains("created") ||
                gcMessage.toLowerCase().contains("成功"))) {
            gcSuccess = true;
        }

        if (gcSuccess) {
            // 6. 记录到IP注册表
            try {
                IpAccountRecord record = new IpAccountRecord(clientIp, username);
                ipAccountRecordRepository.save(record);
                logger.info("账号注册成功，IP记录已保存 - IP: {}, 账号: {}, 该IP已注册: {}/{}",
                        clientIp, username, ipCount + 1, MAX_ACCOUNTS_PER_IP);
                SecurityLogger.logAction(clientIp, null, null, "REGISTER_SUCCESS",
                        "账号注册成功: " + username);
            } catch (Exception e) {
                // 数据库记录失败不影响注册结果，但需要告警
                logger.error("保存IP注册记录失败（账号可能已创建但未被IP计数）- IP: {}, 账号: {}", clientIp, username, e);
            }

            result.put("success", true);
            result.put("message", "账号 " + username + " 注册成功！请使用此账号登录游戏。");
            result.put("username", username);
            result.put("remainingQuota", MAX_ACCOUNTS_PER_IP - (ipCount + 1));
        } else {
            logger.warn("Grasscutter拒绝注册 - IP: {}, 账号: {}, 原因: {}", clientIp, username, gcMessage);
            SecurityLogger.logAction(clientIp, null, null, "REGISTER_FAILED",
                    "GC拒绝: " + username + " | " + gcMessage);

            // 给用户友好的错误提示
            String userMessage;
            if (gcMessage.contains("exist") || gcMessage.contains("已存在")) {
                userMessage = "账号名 " + username + " 已被注册，请换一个名字";
            } else if (gcMessage.contains("invalid") || gcMessage.contains("非法")) {
                userMessage = "账号名不合法，请检查后重试";
            } else {
                userMessage = "注册失败：" + gcMessage;
            }

            result.put("success", false);
            result.put("message", userMessage);
            result.put("gcRetcode", gcResponse.getRetcode());
        }

        return result;
    }

    /**
     * 查询指定IP的注册情况
     */
    public Map<String, Object> getIpQuota(String clientIp) {
        Map<String, Object> result = new HashMap<>();
        long count = ipAccountRecordRepository.countByIpAddress(clientIp);
        result.put("used", count);
        result.put("max", MAX_ACCOUNTS_PER_IP);
        result.put("remaining", Math.max(0, MAX_ACCOUNTS_PER_IP - count));
        result.put("allowed", count < MAX_ACCOUNTS_PER_IP);
        return result;
    }
}
