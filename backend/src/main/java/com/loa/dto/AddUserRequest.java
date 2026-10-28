package com.loa.dto;

public class AddUserRequest {
    private String username;
    private String nickname;
    private String email;
    private String password;
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String v) { username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { nickname = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { password = v; }
    public String getRole() { return role; }
    public void setRole(String v) { role = v; }
}
