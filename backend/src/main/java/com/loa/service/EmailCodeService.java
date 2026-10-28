package com.loa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证码：6 位数字、5 分钟有效、一次性使用。
 *
 * 三层限额，防止有人拿服务器当发信机把 QQ 邮箱配额刷爆：
 *   1. 单邮箱 60 秒冷却、24 小时最多 10 次
 *   2. 单 IP 每小时最多 5 次
 *   3. 全局每日上限（默认 100 封，留足 QQ 邮箱个人版配额余量）
 */
@Service
public class EmailCodeService {

    public static final int TTL_MINUTES = 5;
    public static final int COOLDOWN_SECONDS = 60;
    private static final int DAILY_LIMIT = 10;
    private static final int MAX_FAIL = 5;
    private static final long HOUR = 3600_000L;
    private static final long DAY = 24 * HOUR;

    private static final class Entry {
        String code;
        long expireAt;
        long lastSentAt;
        long windowStart;
        int windowCount;
        int failCount;
    }

    private static final class Window {
        long start;
        int count;
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();
    private final Object quotaLock = new Object();
    private long globalDayStart = 0;
    private int globalDayCount = 0;

    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();
    private final int globalDailyLimit;
    private final int ipHourlyLimit;

    public EmailCodeService(MailService mailService,
                            @Value("${loa.mail.daily-limit:100}") int globalDailyLimit,
                            @Value("${loa.mail.ip-hourly-limit:5}") int ipHourlyLimit) {
        this.mailService = mailService;
        this.globalDailyLimit = globalDailyLimit;
        this.ipHourlyLimit = ipHourlyLimit;
    }

    /** 发送验证码，返回规范化后的邮箱 */
    public String send(String email, String clientIp) {
        if (!mailService.isConfigured()) {
            throw new IllegalArgumentException("邮件服务未配置，请联系管理员设置发件邮箱与授权码");
        }
        String key = normalize(email);
        long now = System.currentTimeMillis();

        acquireGlobalQuota(now);
        acquireIpQuota(clientIp, now);

        Entry e = store.computeIfAbsent(key, k -> new Entry());
        synchronized (e) {
            if (e.lastSentAt > 0) {
                long elapsed = now - e.lastSentAt;
                if (elapsed < COOLDOWN_SECONDS * 1000L) {
                    long wait = (COOLDOWN_SECONDS * 1000L - elapsed) / 1000 + 1;
                    throw new IllegalArgumentException("发送太频繁，请 " + wait + " 秒后再试");
                }
            }
            if (e.windowStart == 0 || now - e.windowStart > DAY) {
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

    /* ---------------- 配额 ---------------- */

    private void acquireGlobalQuota(long now) {
        synchronized (quotaLock) {
            if (globalDayStart == 0 || now - globalDayStart > DAY) {
                globalDayStart = now;
                globalDayCount = 0;
            }
            if (globalDayCount >= globalDailyLimit) {
                throw new IllegalArgumentException("系统今日邮件发送量已达上限（" + globalDailyLimit
                        + " 封），请明天再试");
            }
            globalDayCount++;
        }
    }

    private void acquireIpQuota(String ip, long now) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        // 顺带清理过期窗口，避免 Map 无限增长
        Iterator<Map.Entry<String, Window>> it = ipWindows.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().start > HOUR) {
                it.remove();
            }
        }
        Window w = ipWindows.computeIfAbsent(ip, k -> new Window());
        synchronized (w) {
            if (w.start == 0 || now - w.start > HOUR) {
                w.start = now;
                w.count = 0;
            }
            if (w.count >= ipHourlyLimit) {
                throw new IllegalArgumentException("操作过于频繁，请稍后再试");
            }
            w.count++;
        }
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("请输入邮箱");
        }
        return email.trim().toLowerCase();
    }
}
