package com.loa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 忘记密码：凭注册邮箱 + 邮箱验证码重置密码 */
public class ResetPasswordByEmailRequest {

    @NotBlank(message = "请输入账号")
    @Size(max = 64, message = "账号过长")
    private String username;

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱地址过长")
    private String email;

    @NotBlank(message = "请输入邮箱验证码")
    @Size(max = 8, message = "验证码格式不正确")
    private String emailCode;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位")
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    @Size(min = 6, max = 32, message = "新密码长度需为 6-32 位")
    private String confirmPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailCode() { return emailCode; }
    public void setEmailCode(String emailCode) { this.emailCode = emailCode; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
