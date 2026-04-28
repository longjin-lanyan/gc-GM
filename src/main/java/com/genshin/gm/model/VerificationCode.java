package com.genshin.gm.model;

import java.time.LocalDateTime;

/**
 * 验证码实体类
 * 用于存储UID验证码及其过期时间
 * 使用OpenCommand插件的验证系统
 */
public class VerificationCode {

    private String uid;
    private String token;  // OpenCommand返回的临时token
    private LocalDateTime expiryTime;
    private boolean verified;

    public VerificationCode() {
    }

    public VerificationCode(String uid, String token, LocalDateTime expiryTime) {
        this.uid = uid;
        this.token = token;
        this.expiryTime = expiryTime;
        this.verified = false;
    }

    /**
     * 检查验证码是否已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * 验证码是否有效（已验证且未过期）
     */
    public boolean isValid() {
        return verified && !isExpired();
    }

    // Getters and Setters

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
