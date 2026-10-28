package com.loa.controller;

import com.loa.common.R;
import com.loa.dto.LoginRequest;
import com.loa.dto.RegisterRequest;
import com.loa.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/code")
    public R<Map<String, Object>> code() {
        return R.ok(authService.createCaptcha());
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
