package com.loa.service;

import com.loa.entity.User;
import com.loa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public AuthService(UserRepository userRepository, @Value("${loa.session.ttl-seconds:7200}") long ttlSeconds) {
        this.userRepository = userRepository;
        this.ttlSeconds = ttlSeconds;
    }

    public Map<String, Object> createCaptcha() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Random random = new Random();
        int a = random.nextInt(9) + 1;
        int b = random.nextInt(9) + 1;
        captchaStore.put(uuid, String.valueOf(a + b));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uuid", uuid);
        m.put("img", drawImage(a + " + " + b + " = ?"));
        return m;
    }

    public Map<String, Object> login(String username, String password, String code, String uuid) {
        String expect = uuid == null ? null : captchaStore.remove(uuid);
        if (expect != null && code != null && !code.isBlank() && !expect.equals(code)) {
            throw new IllegalArgumentException("验证码错误");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("access_token", token);
        m.put("expires_in", ttlSeconds);
        return m;
    }

    public User currentUser(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new IllegalArgumentException("未登录或登录已过期");
        }
        String token = authorization.replace("Bearer ", "").trim();
        Long userId = tokenStore.get(token);
        if (userId == null) {
            throw new IllegalArgumentException("未登录或登录已过期");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private String drawImage(String text) {
        int w = 120, h = 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(245, 245, 247));
        g.fillRect(0, 0, w, h);
        g.setFont(new Font("Dialog", Font.BOLD, 20));
        g.setColor(new Color(40, 40, 40));
        g.drawString(text, 10, 27);
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }
}
