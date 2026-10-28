package com.loa.dto;

public class ResetPwdRequest {
    private Long userId;
    private String newPassword;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String v) { newPassword = v; }
}
