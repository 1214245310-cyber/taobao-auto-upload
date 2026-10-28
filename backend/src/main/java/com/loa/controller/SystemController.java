package com.loa.controller;

import com.loa.common.R;
import com.loa.entity.User;
import com.loa.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final AuthService authService;

    public SystemController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/user/getInfo")
    public R<Map<String, Object>> getInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User user = authService.currentUser(authorization);
        Map<String, Object> u = new LinkedHashMap<>();
        u.put("userId", user.getId());
        u.put("userName", user.getUsername());
        u.put("nickName", user.getNickname());
        u.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());
        u.put("email", user.getEmail() == null ? "" : user.getEmail());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", u);
        m.put("roles", List.of("admin"));
        m.put("permissions", List.of("*:*:*"));
        return R.ok(m);
    }
}
