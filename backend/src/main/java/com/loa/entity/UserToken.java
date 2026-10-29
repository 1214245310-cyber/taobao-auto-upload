package com.loa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 登录令牌。
 * 早期版本把 token 放在内存 Map 里，服务一重启（每次部署）全部失效，
 * 用户刷新页面就会看到「未登录或登录已过期」却还停在后台页面。
 * 改为落库后，重启不会掉线，过期时间也真正可控。
 */
@Entity
@Table(name = "user_tokens", indexes = {
        @Index(name = "idx_user_tokens_user", columnList = "user_id"),
        @Index(name = "idx_user_tokens_expires", columnList = "expires_at")
})
public class UserToken {

    @Id
    @Column(length = 64, nullable = false)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public UserToken() {
    }

    public UserToken(String token, Long userId, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
