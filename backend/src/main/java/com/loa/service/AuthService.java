package com.loa.service;

import com.loa.common.UnauthorizedException;
import com.loa.dto.RegisterRequest;
import com.loa.dto.ResetPasswordByEmailRequest;
import com.loa.dto.UpdateUserRequest;
import com.loa.entity.User;
import com.loa.entity.UserToken;
import com.loa.repository.UserRepository;
import com.loa.repository.UserTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailCodeService emailCodeService;
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();
    private final long ttlSeconds;
    /** 连续登录失败达到该次数即锁定 */
    private static final int MAX_LOGIN_FAIL = 5;
    /** 锁定时长（分钟） */
    private static final int LOCK_MINUTES = 10;

    public AuthService(UserRepository userRepository,
                       UserTokenRepository userTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailCodeService emailCodeService,
                       @Value("${loa.session.ttl-seconds:2592000}") long ttlSeconds) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailCodeService = emailCodeService;
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

    /**
     * 注册：用户名与邮箱均唯一，密码 BCrypt 存储，注册即视为邮箱已验证。
     * 校验顺序刻意把「邮箱唯一」放在「验证码校验」之前，
     * 避免用户因为用户名重复白白消耗一次验证码。
     */
    public Map<String, Object> register(RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        String username = req.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名「" + username + "」已被注册");
        }
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱「" + email + "」已被注册");
        }
        // 最后消费验证码
        emailCodeService.verify(email, req.getEmailCode());

        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? username : req.getNickname().trim());
        u.setEmail(email);
        u.setEmailVerified("1");
        u.setRole("USER");
        u.setStatus("0");
        userRepository.save(u);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("email", u.getEmail());
        return m;
    }

    /**
     * 忘记密码第一步：发验证码前先确认邮箱确实注册过，
     * 既避免把服务器当发信机，也让用户少等一封注定没用的邮件。
     */
    public String assertEmailRegistered(String email) {
        String key = email == null ? "" : email.trim().toLowerCase();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("请输入邮箱");
        }
        userRepository.findByEmail(key)
                .orElseThrow(() -> new IllegalArgumentException("该邮箱未注册，请确认是注册时填写的邮箱"));
        return key;
    }

    /**
     * 忘记密码第二步：邮箱验证码 + 新密码。
     * 校验顺序先密码一致性、再账号存在、最后消费验证码，避免密码不一致白白废掉一次验证码。
     */
    public Map<String, Object> resetPasswordByEmail(ResetPasswordByEmailRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        String email = assertEmailRegistered(req.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("该邮箱未注册"));
        emailCodeService.verify(email, req.getEmailCode());

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        // 凭邮箱验证码重置，视为邮箱已确属本人
        user.setEmailVerified("1");
        userRepository.save(user);
        // 旧登录态全部作废，防止密码泄露后对方还挂在线上
        revokeTokens(user.getId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", user.getUsername());
        m.put("nickname", user.getNickname());
        return m;
    }

    public Map<String, Object> login(String username, String password, String code, String uuid, String clientIp) {
        verifyCaptcha(code, uuid);
        User user = userRepository.findByUsername(username == null ? "" : username.trim())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        // 失败次数过多时临时锁定，挡住对着一个账号硬试密码的脚本
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            throw new IllegalArgumentException("登录失败次数过多，请 " + minutes + " 分钟后再试");
        }
        if (!matchesAndMaybeUpgrade(user, password)) {
            int fails = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(fails);
            if (fails >= MAX_LOGIN_FAIL) {
                user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                user.setLoginFailCount(0);
            }
            userRepository.save(user);
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            throw new IllegalArgumentException("该账号已被停用，请联系管理员");
        }
        user.setLastLoginAt(now);
        user.setLastLoginIp(clientIp);
        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = now.plusSeconds(ttlSeconds);
        userTokenRepository.save(new UserToken(token, user.getId(), now, expiresAt));
        // 顺手清理已过期令牌，避免表无限增长
        try {
            userTokenRepository.deleteAll(userTokenRepository.findByExpiresAtBefore(now));
        } catch (Exception ignored) {
            // 清理失败不影响登录
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("access_token", token);
        m.put("expires_in", ttlSeconds);
        return m;
    }

    public void logout(String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            String token = authorization.replace("Bearer ", "").trim();
            userTokenRepository.findById(token).ifPresent(userTokenRepository::delete);
        }
    }

    public User currentUser(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        String token = authorization.replace("Bearer ", "").trim();
        UserToken saved = userTokenRepository.findById(token)
                .orElseThrow(() -> new UnauthorizedException("未登录或登录已过期"));
        if (saved.getExpiresAt() != null && saved.getExpiresAt().isBefore(LocalDateTime.now())) {
            userTokenRepository.delete(saved);
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        return userRepository.findById(saved.getUserId())
                .orElseThrow(() -> new UnauthorizedException("账号不存在，请重新登录"));
    }

    /** 让某个用户的全部登录令牌失效（改密/重置密码后强制重新登录） */
    public void revokeTokens(Long userId) {
        try {
            userTokenRepository.deleteAll(userTokenRepository.findByUserId(userId));
        } catch (Exception ignored) {
            // 忽略：即使清理失败，密码已经变了
        }
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

    /**
     * 用户列表（全量，按 id 倒序，前端自行分页）。
     * 仅返回必要字段，密码等敏感信息绝不外泄。
     */
    public List<Map<String, Object>> listUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            list.add(toUserMap(u));
        }
        return list;
    }

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname() == null ? "" : u.getNickname());
        m.put("email", u.getEmail() == null ? "" : u.getEmail());
        m.put("emailVerified", u.getEmailVerified() == null ? "0" : u.getEmailVerified());
        m.put("avatar", u.getAvatar() == null ? "" : u.getAvatar());
        m.put("role", u.getRole() == null ? "USER" : u.getRole());
        m.put("status", u.getStatus() == null ? "0" : u.getStatus());
        m.put("remark", u.getRemark() == null ? "" : u.getRemark());
        m.put("createdAt", u.getCreatedAt() == null ? "" : u.getCreatedAt().toString());
        m.put("lastLoginAt", u.getLastLoginAt() == null ? "" : u.getLastLoginAt().toString());
        return m;
    }

    /** 用户管理（增删改、重置密码）只对管理员开放 */
    public User requireAdmin(String authorization) {
        User u = currentUser(authorization);
        if (!"ADMIN".equalsIgnoreCase(u.getRole())) {
            throw new IllegalArgumentException("只有管理员可以管理用户");
        }
        return u;
    }

    private static final String EMAIL_RE = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";

    /**
     * 编辑用户：昵称 / 邮箱 / 备注 / 角色 / 状态。
     * 三条保护：不能停用自己、不能把最后一个管理员降级或删除。
     */
    @Transactional
    public Map<String, Object> updateUser(String authorization, UpdateUserRequest req) {
        User operator = requireAdmin(authorization);
        User target = userRepository.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (req.getNickname() != null) {
            String nickname = req.getNickname().trim();
            target.setNickname(nickname.isEmpty() ? target.getUsername() : nickname);
        }

        if (req.getEmail() != null) {
            String email = req.getEmail().trim().toLowerCase();
            String current = target.getEmail() == null ? "" : target.getEmail().toLowerCase();
            if (email.isEmpty()) {
                // 解绑邮箱
                target.setEmail(null);
                target.setEmailVerified("0");
            } else {
                if (!email.matches(EMAIL_RE)) {
                    throw new IllegalArgumentException("邮箱格式不正确");
                }
                if (!email.equals(current) && userRepository.existsByEmail(email)) {
                    throw new IllegalArgumentException("邮箱「" + email + "」已被其他账号使用");
                }
                if (!email.equals(current)) {
                    target.setEmail(email);
                    // 管理员代填的邮箱先视为未验证；用户可用「忘记密码」发码自证
                    target.setEmailVerified("0");
                }
            }
        }

        if (req.getRole() != null) {
            String role = "ADMIN".equalsIgnoreCase(req.getRole()) ? "ADMIN" : "USER";
            boolean downgrade = "ADMIN".equalsIgnoreCase(target.getRole()) && "USER".equals(role);
            if (downgrade && countAdmins() <= 1) {
                throw new IllegalArgumentException("系统至少要保留一个管理员");
            }
            target.setRole(role);
        }

        if (req.getStatus() != null) {
            String status = "1".equals(req.getStatus()) ? "1" : "0";
            if ("1".equals(status)) {
                if (target.getId().equals(operator.getId())) {
                    throw new IllegalArgumentException("不能停用当前登录的账号");
                }
                if ("ADMIN".equalsIgnoreCase(target.getRole()) && countActiveAdmins() <= 1) {
                    throw new IllegalArgumentException("系统至少要保留一个可用管理员");
                }
            }
            target.setStatus(status);
            if ("1".equals(status)) {
                // 停用后立刻踢下线
                revokeTokens(target.getId());
            }
        }

        if (req.getRemark() != null) {
            String remark = req.getRemark().trim();
            target.setRemark(remark.isEmpty() ? null : remark);
        }

        return toUserMap(userRepository.save(target));
    }

    /** 删除用户：不能删自己，也不能删掉最后一个管理员 */
    @Transactional
    public void deleteUser(String authorization, Long userId) {
        User operator = requireAdmin(authorization);
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (target.getId().equals(operator.getId())) {
            throw new IllegalArgumentException("不能删除当前登录的账号");
        }
        if ("ADMIN".equalsIgnoreCase(target.getRole()) && countAdmins() <= 1) {
            throw new IllegalArgumentException("系统至少要保留一个管理员");
        }
        revokeTokens(target.getId());
        userRepository.deleteById(target.getId());
    }

    private long countAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                .count();
    }

    private long countActiveAdmins() {
        return userRepository.findAll().stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                .filter(u -> !"1".equals(u.getStatus()))
                .count();
    }

    /**
     * 管理员后台新增用户（无需邮箱验证码）。
     * 邮箱选填；用户名/密码规则与注册一致。
     */
    public Map<String, Object> addUser(String username, String nickname, String email,
                                       String password, String role) {
        String uname = username == null ? "" : username.trim();
        if (!uname.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("用户名需为 3-20 位字母、数字或下划线");
        }
        if (userRepository.findByUsername(uname).isPresent()) {
            throw new IllegalArgumentException("用户名「" + uname + "」已存在");
        }
        if (password == null || password.length() < 6 || password.length() > 32) {
            throw new IllegalArgumentException("密码长度需为 6-32 位");
        }
        User u = new User();
        u.setUsername(uname);
        u.setPassword(passwordEncoder.encode(password));
        u.setNickname(nickname == null || nickname.isBlank() ? uname : nickname.trim());
        if (email != null && !email.isBlank()) {
            String em = email.trim().toLowerCase();
            if (userRepository.existsByEmail(em)) {
                throw new IllegalArgumentException("邮箱「" + em + "」已被使用");
            }
            u.setEmail(em);
            u.setEmailVerified("0");
        }
        u.setRole("ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER");
        u.setStatus("0");
        userRepository.save(u);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        return m;
    }

    /**
     * 当前登录用户修改自己的密码，需校验原密码。
     * 历史明文密码会在校验时自动升级为 BCrypt。
     */
    public void changePassword(String authorization, String oldPassword, String newPassword) {
        User user = currentUser(authorization);
        if (!matchesAndMaybeUpgrade(user, oldPassword)) {
            throw new IllegalArgumentException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new IllegalArgumentException("新密码长度需为 6-32 位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // 改密后旧登录态全部作废
        revokeTokens(user.getId());
    }

    /**
     * 管理员重置他人密码（不需要原密码），同时清空失败计数与锁定。
     */
    public void resetPassword(Long userId, String newPassword) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            throw new IllegalArgumentException("新密码长度需为 6-32 位");
        }
        u.setPassword(passwordEncoder.encode(newPassword));
        u.setLoginFailCount(0);
        u.setLockedUntil(null);
        userRepository.save(u);
        // 被重置密码的账号需要重新登录
        revokeTokens(u.getId());
    }
}
