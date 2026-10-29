package com.loa.dto;

public class AddUserRequest {
    /** 可留空：留空则自动取邮箱前缀作为登录名 */
    private String username;
    private String nickname;
    private String email;
    /** 绑定邮箱必须的验证码（邮箱必填，且必须验证通过） */
    private String emailCode;
    private String password;
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { nickname = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getEmailCode() { return emailCode; }
    public void setEmailCode(String v) { emailCode = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }
    public String getRole() { return role; }
    public void setRole(String v) { role = v; }
}
