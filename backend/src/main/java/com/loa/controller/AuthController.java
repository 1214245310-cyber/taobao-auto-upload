package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.EmailCodeRequest;
import com.loa.dto.LoginRequest;
import com.loa.dto.RegisterRequest;
import com.loa.service.AuthService;
import com.loa.service.EmailCodeService;
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
    public R<Map<String, Object>> emailCode(@Valid @RequestBody EmailCodeRequest req) {
        String email = emailCodeService.send(req.getEmail());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", email);
        m.put("ttl", EmailCodeService.TTL_MINUTES * 60);
        m.put("cooldown", EmailCodeService.COOLDOWN_SECONDS);
        return R.ok(m);
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest req) {
        return R.ok(authService.login(req.getUsername(), req.getPassword(), req.getCode(), req.getUuid()));
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
}
