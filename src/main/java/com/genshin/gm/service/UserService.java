package com.genshin.gm.service;

import com.genshin.gm.model.User;
import com.genshin.gm.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    // 存储用户session: sessionToken -> username
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    // 存储session过期时间: sessionToken -> expiryTime
    private final Map<String, LocalDateTime> sessionExpiry = new ConcurrentHashMap<>();

    // Session有效期（小时）
    private static final int SESSION_VALIDITY_HOURS = 24;

    /**
     * 用户注册
     */
    public Map<String, Object> register(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 验证输入
            if (username == null || username.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户名不能为空");
                return result;
            }

            if (password == null || password.length() < 6) {
                result.put("success", false);
                result.put("message", "密码至少6个字符");
                return result;
            }

            username = username.trim();

            // 检查用户名是否已存在
            if (userRepository.existsByUsername(username)) {
                result.put("success", false);
                result.put("message", "用户名已存在");
                return result;
            }

            // 加密密码
            String hashedPassword = hashPassword(password);

            // 创建用户
            User user = new User(username, hashedPassword);
            userRepository.save(user);

            logger.info("用户注册成功: {}", username);

            result.put("success", true);
            result.put("message", "注册成功");
            result.put("username", username);

        } catch (Exception e) {
            logger.error("注册失败", e);
            result.put("success", false);
            result.put("message", "注册失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (username == null || username.trim().isEmpty() || password == null) {
                result.put("success", false);
                result.put("message", "用户名或密码不能为空");
                return result;
            }

            username = username.trim();

            // 查找用户
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户名或密码错误");
                return result;
            }

            User user = userOpt.get();

            // 验证密码
            String hashedPassword = hashPassword(password);
            if (!hashedPassword.equals(user.getPassword())) {
                result.put("success", false);
                result.put("message", "用户名或密码错误");
                return result;
            }

            // 更新最后登录时间
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // 创建session
            String sessionToken = generateSessionToken();
            sessions.put(sessionToken, username);
            sessionExpiry.put(sessionToken, LocalDateTime.now().plusHours(SESSION_VALIDITY_HOURS));

            logger.info("用户登录成功: {}", username);

            result.put("success", true);
            result.put("message", "登录成功");
            result.put("sessionToken", sessionToken);
            result.put("username", username);
            result.put("verifiedUids", user.getVerifiedUids());

        } catch (Exception e) {
            logger.error("登录失败", e);
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 验证session
     */
    public String validateSession(String sessionToken) {
        if (sessionToken == null || !sessions.containsKey(sessionToken)) {
            return null;
        }

        // 检查是否过期
        LocalDateTime expiry = sessionExpiry.get(sessionToken);
        if (expiry == null || LocalDateTime.now().isAfter(expiry)) {
            sessions.remove(sessionToken);
            sessionExpiry.remove(sessionToken);
            return null;
        }

        return sessions.get(sessionToken);
    }

    /**
     * 登出
     */
    public void logout(String sessionToken) {
        if (sessionToken != null) {
            sessions.remove(sessionToken);
            sessionExpiry.remove(sessionToken);
        }
    }

    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(String sessionToken) {
        Map<String, Object> result = new HashMap<>();

        String username = validateSession(sessionToken);
        if (username == null) {
            result.put("success", false);
            result.put("message", "未登录或session已过期");
            return result;
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }

        User user = userOpt.get();
        result.put("success", true);
        result.put("username", user.getUsername());
        result.put("verifiedUids", user.getVerifiedUids());
        result.put("createdAt", user.getCreatedAt().toString());

        return result;
    }

    /**
     * 添加已验证的UID
     */
    public boolean addVerifiedUid(String username, String uid) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return false;
            }

            User user = userOpt.get();
            user.addVerifiedUid(uid);
            userRepository.save(user);

            logger.info("用户 {} 添加已验证UID: {}", username, uid);
            return true;

        } catch (Exception e) {
            logger.error("添加已验证UID失败", e);
            return false;
        }
    }

    /**
     * 获取用户绑定的UID列表（字符串形式，用于日志）
     */
    public String getVerifiedUidsString(String username) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return "用户不存在";
            }
            List<String> uids = userOpt.get().getVerifiedUids();
            if (uids == null || uids.isEmpty()) {
                return "无绑定UID";
            }
            return String.join(", ", uids);
        } catch (Exception e) {
            return "获取失败";
        }
    }

    /**
     * 检查用户是否已验证某个UID
     */
    public boolean isUidVerified(String username, String uid) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return false;
            }

            return userOpt.get().isUidVerified(uid);

        } catch (Exception e) {
            logger.error("检查UID验证状态失败", e);
            return false;
        }
    }

    /**
     * 移除已验证的UID
     */
    public boolean removeVerifiedUid(String username, String uid) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return false;
            }

            User user = userOpt.get();
            user.removeVerifiedUid(uid);
            userRepository.save(user);

            logger.info("用户 {} 移除已验证UID: {}", username, uid);
            return true;

        } catch (Exception e) {
            logger.error("移除已验证UID失败", e);
            return false;
        }
    }

    /**
     * MD5加密密码
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    /**
     * 生成session token
     */
    private String generateSessionToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
}
