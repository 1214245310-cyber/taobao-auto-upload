package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.EmailCodeRequest;
import com.loa.dto.LoginRequest;
import com.loa.dto.RegisterRequest;
import com.loa.dto.ResetPasswordByEmailRequest;
import com.loa.service.AuthService;
import com.loa.service.EmailCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailCodeService emailCodeService;

    public AuthController(AuthService authService, EmailCodeService emailCodeService) {
        this.authService = authService;
        this.emailCodeService = emailCodeService;
    }

    @GetMapping("/code")
    public R<Map<String, Object>> code() {
        return R.ok(authService.createCaptcha());
    }

    /** 发送邮箱验证码（QQ 邮箱 SMTP） */
    @PostMapping("/email-code")
    public R<Map<String, Object>> emailCode(@Valid @RequestBody EmailCodeRequest req,
                                            HttpServletRequest request) {
        String email = emailCodeService.send(req.getEmail(), clientIp(request));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", email);
        m.put("ttl", EmailCodeService.TTL_MINUTES * 60);
        m.put("cooldown", EmailCodeService.COOLDOWN_SECONDS);
        return R.ok(m);
    }

    /**
     * 忘记密码第一步：向注册邮箱发验证码。
     * 与注册用的 /email-code 分开，先校验邮箱是否已注册，且前端可提示"未注册"。
     */
    @PostMapping("/password/send-code")
    public R<Map<String, Object>> sendResetCode(@Valid @RequestBody EmailCodeRequest req,
                                                HttpServletRequest request) {
        String email = authService.assertEmailRegistered(req.getEmail());
        emailCodeService.send(email, clientIp(request));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", email);
        m.put("ttl", EmailCodeService.TTL_MINUTES * 60);
        m.put("cooldown", EmailCodeService.COOLDOWN_SECONDS);
        return R.ok(m);
    }

    /** 忘记密码第二步：邮箱验证码校验通过后重置密码 */
    @PostMapping("/password/reset")
    public R<Map<String, Object>> resetPasswordByEmail(
            @Valid @RequestBody ResetPasswordByEmailRequest req) {
        return R.ok(authService.resetPasswordByEmail(req));
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        return R.ok(authService.login(req.getUsername(), req.getPassword(), req.getCode(), req.getUuid(),
                clientIp(request)));
    }

    @PostMapping("/register")
    public R<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return R.ok();
    }

    /** 取真实客户端 IP，Caddy 反代会带 X-Forwarded-For */
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
}
