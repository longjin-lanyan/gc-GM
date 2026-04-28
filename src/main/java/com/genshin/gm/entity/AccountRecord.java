package com.genshin.gm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 账号注册记录实体
 */
@Entity
@Table(name = "account_records")
public class AccountRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 15)
    private String qq;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AccountRecord() {}

    public AccountRecord(String username, String qq) {
        this.username = username;
        this.qq = qq;
        this.createdAt = LocalDateTime.now();
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getQq() { return qq; }
    public void setQq(String qq) { this.qq = qq; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}