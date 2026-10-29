package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.AddUserRequest;
import com.loa.dto.ChangePwdRequest;
import com.loa.dto.ResetPwdRequest;
import com.loa.dto.UpdateUserRequest;
import com.loa.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /** 用户管理整体只对管理员开放：列表、新增、编辑、删除、重置密码 */
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireAdmin(authorization);
        return R.ok(authService.listUsers());
    }

    @PostMapping("/add")
    public R<Map<String, Object>> add(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AddUserRequest req) {
        authService.requireAdmin(authorization);
        return R.ok(authService.addUser(req.getUsername(), req.getNickname(),
                req.getEmail(), req.getPassword(), req.getRole()));
    }

    /** 编辑用户：昵称 / 邮箱 / 备注 / 角色 / 状态 */
    @PostMapping("/update")
    public R<Map<String, Object>> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateUserRequest req) {
        return R.ok(authService.updateUser(authorization, req));
    }

    @PostMapping("/delete")
    public R<Void> delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Object id = body.get("userId");
        if (id == null) throw new IllegalArgumentException("缺少 userId");
        authService.deleteUser(authorization, Long.valueOf(String.valueOf(id)));
        return R.ok();
    }

    @PostMapping("/reset-pwd")
    public R<Void> resetPwd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ResetPwdRequest req) {
        authService.requireAdmin(authorization);
        authService.resetPassword(req.getUserId(), req.getNewPassword());
        return R.ok();
    }

    /** 修改自己的密码，任何登录用户都可以 */
    @PostMapping("/change-pwd")
    public R<Void> changePwd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChangePwdRequest req) {
        authService.changePassword(authorization, req.getOldPassword(), req.getNewPassword());
        return R.ok();
    }
}
