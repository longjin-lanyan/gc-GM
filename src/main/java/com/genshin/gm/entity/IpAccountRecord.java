package com.genshin.gm.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * IP注册记录实体 - 记录每个IP创建的账号数量
 */
@Entity
@Table(name = "ip_account_records", indexes = {
        @Index(name = "idx_ip_address", columnList = "ip_address")
})
public class IpAccountRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public IpAccountRecord() {}

    public IpAccountRecord(String ipAddress, String username) {
        this.ipAddress = ipAddress;
        this.username = username;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
