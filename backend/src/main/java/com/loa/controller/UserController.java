package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.AddUserRequest;
import com.loa.dto.ChangePwdRequest;
import com.loa.dto.ResetPwdRequest;
import com.loa.dto.UpdateUserRequest;
import com.loa.service.AuthService;
import com.loa.service.EmailCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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
                req.getEmail(), req.getPassword(), req.getRole(), req.getEmailCode()));
    }

    /** 给「要绑定的邮箱」发验证码：新增用户或换绑邮箱前必须先验证 */
    @PostMapping("/email-code")
    public R<Map<String, Object>> emailCode(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        authService.requireAdmin(authorization);
        Object raw = body.get("email");
        String email = raw == null ? "" : String.valueOf(raw).trim().toLowerCase();
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        if (authService.isEmailTaken(email)) {
            throw new IllegalArgumentException("邮箱「" + email + "」已被其他账号使用");
        }
        authService.sendBindCode(email, clientIp(request));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", email);
        m.put("ttl", EmailCodeService.TTL_MINUTES * 60);
        m.put("cooldown", EmailCodeService.COOLDOWN_SECONDS);
        return R.ok(m);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
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
