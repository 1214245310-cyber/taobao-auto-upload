package com.loa.dto;

public class ChangePwdRequest {
    private String oldPassword;
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String v) { oldPassword = v; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String v) { newPassword = v; }
}
