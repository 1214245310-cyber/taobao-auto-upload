package com.loa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailCodeRequest {

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱地址过长")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
