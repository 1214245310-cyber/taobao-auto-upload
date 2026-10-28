package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.AddUserRequest;
import com.loa.dto.ChangePwdRequest;
import com.loa.dto.ResetPwdRequest;
import com.loa.service.AuthService;
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

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.currentUser(authorization); // 校验已登录，避免用户列表被公网裸刷
        return R.ok(authService.listUsers());
    }

    @PostMapping("/add")
    public R<Map<String, Object>> add(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AddUserRequest req) {
        authService.currentUser(authorization);
        return R.ok(authService.addUser(req.getUsername(), req.getNickname(),
                req.getEmail(), req.getPassword(), req.getRole()));
    }

    @PostMapping("/reset-pwd")
    public R<Void> resetPwd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ResetPwdRequest req) {
        authService.currentUser(authorization);
        authService.resetPassword(req.getUserId(), req.getNewPassword());
        return R.ok();
    }

    @PostMapping("/change-pwd")
    public R<Void> changePwd(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChangePwdRequest req) {
        authService.changePassword(authorization, req.getOldPassword(), req.getNewPassword());
        return R.ok();
    }
}
