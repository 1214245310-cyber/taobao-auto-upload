package com.loa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 忘记密码第一步：账号 + 注册邮箱，两者必须属于同一个用户才发验证码 */
public class ForgotPasswordCodeRequest {

    @NotBlank(message = "请输入账号")
    @Size(max = 64, message = "账号过长")
    private String username;

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱地址过长")
    private String email;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
