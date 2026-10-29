package com.loa.dto;

import jakarta.validation.constraints.NotNull;

/** 编辑用户：只改资料与角色/状态，用户名与密码走各自接口 */
public class UpdateUserRequest {

    @NotNull(message = "缺少用户 id")
    private Long id;

    private String nickname;

    /** 空字符串表示解绑邮箱 */
    private String email;

    /** 改成新邮箱时必填：该邮箱收到的验证码 */
    private String emailCode;

    /** ADMIN / USER */
    private String role;

    /** "0" 正常、"1" 停用 */
    private String status;

    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailCode() { return emailCode; }
    public void setEmailCode(String emailCode) { this.emailCode = emailCode; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
