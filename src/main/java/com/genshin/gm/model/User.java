package com.genshin.gm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户模型
 */
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;  // 用户名（唯一）

    @Column(nullable = false)
    private String password;  // 密码（加密存储）

    private LocalDateTime createdAt;  // 注册时间

    private LocalDateTime lastLoginAt;  // 最后登录时间

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_verified_uids",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "uid"})
    )
    @Column(name = "uid")
    private Set<String> verifiedUids;  // 已验证的UID集合

    public User() {
        this.createdAt = LocalDateTime.now();
        this.verifiedUids = new LinkedHashSet<>();
    }

    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Set<String> getVerifiedUids() {
        return verifiedUids;
    }

    public void setVerifiedUids(Set<String> verifiedUids) {
        this.verifiedUids = verifiedUids;
    }

    /**
     * 添加已验证的UID
     */
    public void addVerifiedUid(String uid) {
        if (this.verifiedUids == null) {
            this.verifiedUids = new LinkedHashSet<>();
        }
        this.verifiedUids.add(uid);
    }

    /**
     * 检查UID是否已验证
     */
    public boolean isUidVerified(String uid) {
        return this.verifiedUids != null && this.verifiedUids.contains(uid);
    }

    /**
     * 移除已验证的UID
     */
    public void removeVerifiedUid(String uid) {
        if (this.verifiedUids != null) {
            this.verifiedUids.remove(uid);
        }
    }
}
