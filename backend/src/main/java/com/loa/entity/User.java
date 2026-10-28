package com.loa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(length = 64)
    private String nickname;

    /** 邮箱：注册时必填，历史账号可能为空，故不做非空约束 */
    @Column(length = 128)
    private String email;

    /** 邮箱是否已验证，"1" 已验证、"0" 未验证 */
    @Column(length = 8)
    private String emailVerified;

    /** 头像地址，暂时只有前端默认头像，留作上传功能使用 */
    @Column(length = 256)
    private String avatar;

    /** 管理员备注，例如「财务-李四」，方便在用户列表里认人 */
    @Column(length = 255)
    private String remark;

    /** 最近一次登录时间 */
    private LocalDateTime lastLoginAt;

    /** 最近一次登录 IP，异常登录时用于排查 */
    @Column(length = 64)
    private String lastLoginIp;

    /** 连续登录失败次数，登录成功即清零 */
    private Integer loginFailCount;

    /** 锁定截止时间，失败次数过多时置为「当前时间 + 10 分钟」 */
    private LocalDateTime lockedUntil;

    @Column(length = 32)
    private String role;

    @Column(length = 16)
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = "0";
        if (emailVerified == null) emailVerified = "0";
        if (loginFailCount == null) loginFailCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailVerified() { return emailVerified; }
    public void setEmailVerified(String emailVerified) { this.emailVerified = emailVerified; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public Integer getLoginFailCount() { return loginFailCount; }
    public void setLoginFailCount(Integer loginFailCount) { this.loginFailCount = loginFailCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
