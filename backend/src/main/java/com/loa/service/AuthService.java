package com.loa.service;

import com.loa.dto.RegisterRequest;
import com.loa.entity.User;
import com.loa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${loa.session.ttl-seconds:7200}") long ttlSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    /** 注册：用户名唯一，密码 BCrypt 存储，默认角色 USER、状态正常 */
    public Map<String, Object> register(RegisterRequest req) {
        verifyCaptcha(req.getCode(), req.getUuid());
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        String username = req.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名「" + username + "」已被注册");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? username : req.getNickname().trim());
        u.setRole("USER");
        u.setStatus("0");
        userRepository.save(u);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        return m;
    }

    public Map<String, Object> login(String username, String password, String code, String uuid) {
        verifyCaptcha(code, uuid);
        User user = userRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!matchesAndMaybeUpgrade(user, password)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            throw new IllegalArgumentException("该账号已被停用，请联系管理员");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("access_token", token);
        m.put("expires_in", ttlSeconds);
        return m;
    }

    public void logout(String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            tokenStore.remove(authorization.replace("Bearer ", "").trim());
        }
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

    /** 校验并立即失效验证码 */
    private void verifyCaptcha(String code, String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("验证码已失效，请刷新后重试");
        }
        String expect = captchaStore.remove(uuid);
        if (expect == null) {
            throw new IllegalArgumentException("验证码已失效，请刷新后重试");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("请输入验证码");
        }
        if (!expect.equals(code.trim())) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    /**
     * 校验密码。历史数据里 admin 等账号是明文，比对成功后自动升级为 BCrypt，
     * 下次登录起就走哈希校验。
     */
    private boolean matchesAndMaybeUpgrade(User user, String rawPassword) {
        String stored = user.getPassword();
        if (stored == null) return false;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, stored);
        }
        if (!stored.equals(rawPassword)) return false;
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return true;
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
