package com.genshin.gm.service;

import com.genshin.gm.config.ConfigLoader;
import com.genshin.gm.model.OpenCommandResponse;
import com.genshin.gm.model.User;
import com.genshin.gm.repository.IpAccountRecordRepository;
import com.genshin.gm.repository.UserRepository;
import com.genshin.gm.util.SecurityLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 账号注销服务
 * 支持三种模式：
 *   1. 仅注销后台账号（删除 users 表记录）
 *   2. 仅注销游戏账号（执行 GC account delete 命令）
 *   3. 全部注销（默认，两者均删除）
 * 注销后同步减少该 IP 的注册配额计数。
 */
@Service
public class DeleteAccountService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteAccountService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private IpAccountRecordRepository ipAccountRecordRepository;
    @Autowired private GrasscutterService grasscutterService;
    @Autowired private UserService userService;

    /**
     * 注销账号入口
     *
     * @param sessionToken  当前登录的 session token
     * @param deleteBackend 是否删除后台账号
     * @param deleteGame    是否删除游戏账号
     * @param clientIp      客户端 IP
     */
    @Transactional
    public Map<String, Object> deleteAccount(String sessionToken,
                                             boolean deleteBackend,
                                             boolean deleteGame,
                                             String clientIp) {
        Map<String, Object> result = new HashMap<>();

        // 1. 验证 session
        String username = userService.validateSession(sessionToken);
        if (username == null) {
            result.put("success", false);
            result.put("message", "未登录或 session 已过期，请重新登录");
            return result;
        }

        // 2. 获取用户
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "账号不存在");
            return result;
        }

        if (!deleteBackend && !deleteGame) {
            result.put("success", false);
            result.put("message", "请至少选择一种注销类型");
            return result;
        }

        List<String> successList = new ArrayList<>();
        List<String> failList    = new ArrayList<>();

        // 3. 删除游戏账号（先删游戏，再删后台，避免后台删了找不到用户名）
        if (deleteGame) {
            String err = deleteGameAccount(username, clientIp);
            if (err == null) {
                successList.add("游戏账号");
                logger.info("游戏账号删除成功 - 用户: {}", username);
            } else {
                failList.add("游戏账号（" + err + "）");
                logger.warn("游戏账号删除失败 - 用户: {}, 原因: {}", username, err);
            }
        }

        // 4. 删除后台账号
        if (deleteBackend) {
            try {
                userService.logout(sessionToken);           // 先作废 session
                userRepository.deleteById(userOpt.get().getId());  // 级联删除 user_verified_uids
                successList.add("后台账号");
                logger.info("后台账号删除成功 - 用户: {}", username);
            } catch (Exception e) {
                failList.add("后台账号（" + e.getMessage() + "）");
                logger.error("后台账号删除异常 - 用户: {}", username, e);
            }
        }

        // 5. 减少 IP 注册配额（任意一项成功即执行）
        if (!successList.isEmpty()) {
            try {
                // deleteByUsername 删除该用户名在 ip_account_records 里的所有记录（通常只有1条）
                ipAccountRecordRepository.deleteByUsername(username);
                logger.info("IP注册计数已减少 - 用户: {}, IP: {}", username, clientIp);
            } catch (Exception e) {
                logger.error("减少IP注册计数失败 - 用户: {}", username, e);
            }

            SecurityLogger.logAction(clientIp, username, null, "ACCOUNT_DELETE",
                    "注销成功: " + String.join(", ", successList));
        }

        // 6. 组装响应
        boolean sessionInvalidated = deleteBackend && successList.contains("后台账号");

        if (failList.isEmpty()) {
            result.put("success", true);
            result.put("message", "已成功注销：" + String.join("、", successList));
            result.put("deleted", successList);
            result.put("sessionInvalidated", sessionInvalidated);
        } else if (!successList.isEmpty()) {
            result.put("success", true);
            result.put("partial", true);
            result.put("message",
                    "部分注销成功。成功：" + String.join("、", successList)
                    + "；失败：" + String.join("、", failList));
            result.put("deleted", successList);
            result.put("sessionInvalidated", sessionInvalidated);
        } else {
            result.put("success", false);
            result.put("message", "注销失败：" + String.join("、", failList));
        }
        return result;
    }

    /**
     * 执行 GC 游戏账号删除命令，返回 null 表示成功，否则返回错误描述。
     */
    private String deleteGameAccount(String username, String clientIp) {
        String serverUrl    = ConfigLoader.getConfig().getGrasscutter().getFullUrl();
        String consoleToken = ConfigLoader.getConfig().getGrasscutter().getConsoleToken();

        if (consoleToken == null || consoleToken.isEmpty()) {
            return "服务器 consoleToken 未配置";
        }

        OpenCommandResponse resp = grasscutterService.executeConsoleCommand(
                serverUrl, consoleToken, "account delete " + username,
                clientIp, "ACCOUNT_DELETE", username
        );

        if (resp == null) return "GC 服务器无响应";

        boolean ok = resp.getRetcode() == 0 || resp.isSuccess();
        String  msg = resp.getMessage() != null ? resp.getMessage() : "";

        if (!ok && (msg.toLowerCase().contains("deleted") ||
                    msg.toLowerCase().contains("成功") ||
                    msg.toLowerCase().contains("delete"))) {
            ok = true;
        }

        return ok ? null : ("GC 返回: " + msg);
    }
}
