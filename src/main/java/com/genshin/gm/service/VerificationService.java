package com.genshin.gm.service;

import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.model.VerificationCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码服务
 * 管理UID验证码的生成、验证和过期清理
 */
@Service
public class VerificationService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);

    // 验证码发送后的有效期（分钟）- 用户需在此时间内输入验证码
    private static final int CODE_EXPIRY_MINUTES = 2;

    // 验证成功后的有效期（分钟）- 绑定UID操作需在此时间内完成
    private static final int VERIFIED_EXPIRY_MINUTES = 10;

    // 存储验证信息的Map: UID -> VerificationCode
    private final Map<String, VerificationCode> verificationCodes = new ConcurrentHashMap<>();

    @Autowired
    private GrasscutterService grasscutterService;

    /**
     * 发送验证码到玩家（通过OpenCommand的sendCode API）
     * @param uid 玩家UID
     * @return 操作结果
     */
    public Map<String, Object> sendVerificationCode(String uid) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        try {
            int uidInt = Integer.parseInt(uid);
            LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);

            // 调用OpenCommand的sendCode API
            String serverUrl = ConfigLoader.getConfig().getGrasscutter().getFullUrl();

            logger.info("为UID {} 调用sendCode API", uid);

            OpenCommandResponse response = grasscutterService.sendCode(serverUrl, uidInt);

            logger.info("sendCode响应 - retcode: {}, message: {}, data: {}",
                response != null ? response.getRetcode() : "null",
                response != null ? response.getMessage() : "null",
                response != null ? response.getData() : "null"
            );

            if (response != null && response.getRetcode() == 200) {
                // OpenCommand返回token，保存它
                String token = response.getData() != null ? response.getData().toString() : null;

                if (token == null || token.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "未收到token，请确保玩家在线");
                    return result;
                }

                // 存储token和过期时间
                VerificationCode verificationCode = new VerificationCode(uid, token, expiryTime);
                verificationCodes.put(uid, verificationCode);

                logger.info("UID {} 的验证码已发送，token已保存，过期时间: {}", uid, expiryTime);

                result.put("success", true);
                result.put("message", "验证码已发送，请在游戏中查看（4位数字）");
                result.put("expiryMinutes", CODE_EXPIRY_MINUTES);
            } else {
                result.put("success", false);
                String errorMsg = response != null ? response.getMessage() : "未知错误";
                if (errorMsg.contains("not online") || errorMsg.contains("离线")) {
                    result.put("message", "玩家不在线，请先登录游戏");
                } else {
                    result.put("message", "发送失败: " + errorMsg);
                }
                logger.error("sendCode失败 - response: {}", response);
            }

        } catch (NumberFormatException e) {
            logger.error("UID格式错误: {}", uid, e);
            result.put("success", false);
            result.put("message", "UID格式错误，请输入数字");
        } catch (Exception e) {
            logger.error("发送验证码异常", e);
            verificationCodes.remove(uid);
            result.put("success", false);
            result.put("message", "发送验证码失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证验证码（通过OpenCommand的verify API）
     * @param uid 玩家UID
     * @param code 输入的验证码（4位数字）
     * @return 验证结果
     */
    public Map<String, Object> verifyCode(String uid, String code) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        try {
            VerificationCode storedInfo = verificationCodes.get(uid);

            if (storedInfo == null) {
                result.put("success", false);
                result.put("message", "请先发送验证码");
                return result;
            }

            if (storedInfo.isExpired()) {
                verificationCodes.remove(uid);
                result.put("success", false);
                result.put("message", "验证码已过期，请重新获取");
                return result;
            }

            // 调用OpenCommand的verify API
            String serverUrl = ConfigLoader.getConfig().getGrasscutter().getFullUrl();
            String token = storedInfo.getToken();
            int codeInt = Integer.parseInt(code);

            logger.info("验证UID {} 的验证码: {}", uid, code);

            OpenCommandResponse response = grasscutterService.verifyCode(serverUrl, token, codeInt);

            logger.info("verify响应 - retcode: {}, message: {}",
                response != null ? response.getRetcode() : "null",
                response != null ? response.getMessage() : "null"
            );

            if (response != null && response.getRetcode() == 200) {
                // 验证成功，标记为已验证，并延长过期时间
                storedInfo.setVerified(true);
                storedInfo.setExpiryTime(LocalDateTime.now().plusMinutes(VERIFIED_EXPIRY_MINUTES));
                result.put("success", true);
                result.put("message", "验证成功");
                result.put("expiryTime", storedInfo.getExpiryTime().toString());
                logger.info("UID {} 验证成功，session有效期延长至 {}", uid, storedInfo.getExpiryTime());
            } else {
                result.put("success", false);
                String errorMsg = response != null ? response.getMessage() : "未知错误";
                result.put("message", "验证失败: " + errorMsg);
                logger.warn("UID {} 验证失败: {}", uid, errorMsg);
            }

        } catch (NumberFormatException e) {
            result.put("success", false);
            result.put("message", "验证码格式错误，请输入4位数字");
            logger.error("验证码格式错误: {}", code, e);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "验证失败: " + e.getMessage());
            logger.error("验证异常", e);
        }

        return result;
    }

    /**
     * 检查UID是否已验证（后端强制检查过期时间）
     * @param uid 玩家UID
     * @return 是否已验证且未过期
     */
    public boolean isVerified(String uid) {
        VerificationCode code = verificationCodes.get(uid);
        if (code == null) {
            return false;
        }

        // 后端强制检查过期时间
        if (code.isExpired()) {
            verificationCodes.remove(uid);
            logger.info("UID {} 的临时验证已过期，已清除", uid);
            return false;
        }

        return code.isVerified();
    }

    /**
     * 获取验证状态
     * @param uid 玩家UID
     * @return 验证状态信息
     */
    public Map<String, Object> getVerificationStatus(String uid) {
        Map<String, Object> result = new ConcurrentHashMap<>();

        VerificationCode code = verificationCodes.get(uid);

        if (code == null) {
            result.put("verified", false);
            result.put("message", "未验证");
            return result;
        }

        if (code.isExpired()) {
            verificationCodes.remove(uid);
            result.put("verified", false);
            result.put("message", "验证已过期");
            return result;
        }

        result.put("verified", code.isVerified());
        result.put("expiryTime", code.getExpiryTime().toString());
        result.put("message", code.isVerified() ? "已验证" : "待验证");

        return result;
    }

    /**
     * 获取已验证的token（后端强制检查过期时间）
     * @param uid 玩家UID
     * @return token，如果未验证或已过期返回null
     */
    public String getVerifiedToken(String uid) {
        VerificationCode code = verificationCodes.get(uid);
        if (code == null || !code.isVerified()) {
            return null;
        }
        if (code.isExpired()) {
            verificationCodes.remove(uid);
            return null;
        }
        return code.getToken();
    }

    /**
     * 定时清理过期的验证码（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredCodes() {
        int removed = 0;
        for (Map.Entry<String, VerificationCode> entry : verificationCodes.entrySet()) {
            if (entry.getValue().isExpired()) {
                verificationCodes.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            logger.info("清理了 {} 个过期验证码", removed);
        }
    }
}
