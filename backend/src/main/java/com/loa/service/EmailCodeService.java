package com.loa.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码：6 位数字、5 分钟有效、一次性使用。
 * 单邮箱 60 秒冷却、24 小时最多 10 次，防刷。
 */
@Service
public class EmailCodeService {

    public static final int TTL_MINUTES = 5;
    public static final int COOLDOWN_SECONDS = 60;
    private static final int DAILY_LIMIT = 10;
    private static final int MAX_FAIL = 5;

    private static final class Entry {
        String code;
        long expireAt;
        long lastSentAt;
        long windowStart;
        int windowCount;
        int failCount;
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public EmailCodeService(MailService mailService) {
        this.mailService = mailService;
    }

    /** 发送验证码，返回规范化后的邮箱 */
    public String send(String email) {
        if (!mailService.isConfigured()) {
            throw new IllegalArgumentException("邮件服务未配置，请联系管理员设置发件邮箱与授权码");
        }
        String key = normalize(email);
        long now = System.currentTimeMillis();
        Entry e = store.computeIfAbsent(key, k -> new Entry());
        synchronized (e) {
            if (e.lastSentAt > 0) {
                long elapsed = now - e.lastSentAt;
                if (elapsed < COOLDOWN_SECONDS * 1000L) {
                    long wait = (COOLDOWN_SECONDS * 1000L - elapsed) / 1000 + 1;
                    throw new IllegalArgumentException("发送太频繁，请 " + wait + " 秒后再试");
                }
            }
            if (e.windowStart == 0 || now - e.windowStart > 24L * 3600 * 1000) {
                e.windowStart = now;
                e.windowCount = 0;
            }
            if (e.windowCount >= DAILY_LIMIT) {
                throw new IllegalArgumentException("该邮箱今日获取验证码次数已达上限，请明天再试");
            }
            String code = String.format("%06d", random.nextInt(1_000_000));
            // 发送失败会抛出异常，此时不更新状态，用户可立即重试
            mailService.sendCode(key, code, TTL_MINUTES);
            e.code = code;
            e.expireAt = now + TTL_MINUTES * 60_000L;
            e.lastSentAt = now;
            e.windowCount++;
            e.failCount = 0;
        }
        return key;
    }

    /** 校验并消费验证码 */
    public void verify(String email, String rawCode) {
        String key = normalize(email);
        Entry e = store.get(key);
        if (e == null) {
            throw new IllegalArgumentException("请先获取邮箱验证码");
        }
        synchronized (e) {
            if (e.expireAt < System.currentTimeMillis()) {
                store.remove(key);
                throw new IllegalArgumentException("邮箱验证码已过期，请重新获取");
            }
            String input = rawCode == null ? "" : rawCode.trim();
            if (!e.code.equals(input)) {
                e.failCount++;
                if (e.failCount >= MAX_FAIL) {
                    store.remove(key);
                    throw new IllegalArgumentException("验证码错误次数过多，请重新获取");
                }
                throw new IllegalArgumentException("邮箱验证码错误");
            }
            store.remove(key);
        }
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("请输入邮箱");
        }
        return email.trim().toLowerCase();
    }
}
