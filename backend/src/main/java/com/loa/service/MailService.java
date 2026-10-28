package com.loa.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * QQ 邮箱 SMTP 发信。
 * 账号与授权码通过环境变量注入（LOA_MAIL_USERNAME / LOA_MAIL_PASSWORD），不落仓库。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final String from;
    private final String fromName;

    public MailService(JavaMailSender sender,
                       @Value("${loa.mail.from:}") String from,
                       @Value("${loa.mail.from-name:LOA}") String fromName) {
        this.sender = sender;
        this.from = from == null ? "" : from.trim();
        this.fromName = fromName;
    }

    /** 是否已配置发件邮箱 */
    public boolean isConfigured() {
        return !from.isEmpty();
    }

    public void sendCode(String to, String code, int ttlMinutes) {
        if (!isConfigured()) {
            throw new IllegalArgumentException("邮件服务未配置，请联系管理员设置发件邮箱与授权码");
        }
        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            // 发件地址必须与 SMTP 认证账号一致，否则 QQ 邮箱会拒信
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject("【LOA】邮箱验证码");
            helper.setText(buildHtml(code, ttlMinutes), true);
            sender.send(mime);
            log.info("邮箱验证码已发送 -> {}", mask(to));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("发送邮件失败 -> {}", mask(to), e);
            String reason = (e.getMessage() == null || e.getMessage().isBlank())
                    ? e.getClass().getSimpleName() : e.getMessage();
            throw new IllegalArgumentException("邮件发送失败：" + reason
                    + "（请检查 QQ 邮箱授权码是否正确、SMTP 服务是否已开启）");
        }
    }

    private String buildHtml(String code, int ttlMinutes) {
        return "<div style=\"font-family:-apple-system,'Microsoft YaHei',Arial,sans-serif;max-width:520px;margin:0 auto;"
                + "border:1px solid #e5e7eb;border-radius:10px;overflow:hidden\">"
                + "<div style=\"background:#1f2937;color:#fff;padding:18px 24px;font-size:16px;letter-spacing:3px\">"
                + "LOA · 淘宝自动上货系统</div>"
                + "<div style=\"padding:26px 24px\">"
                + "<p style=\"margin:0 0 14px;color:#374151;font-size:14px\">您好，您正在注册 LOA 账号，本次验证码为：</p>"
                + "<div style=\"font-size:30px;font-weight:700;letter-spacing:8px;color:#4f46e5;background:#f3f4f6;"
                + "padding:14px;text-align:center;border-radius:8px\">" + code + "</div>"
                + "<p style=\"margin:16px 0 0;color:#9ca3af;font-size:12px\">验证码 " + ttlMinutes + " 分钟内有效，请勿泄露给他人。</p>"
                + "<p style=\"margin:6px 0 0;color:#9ca3af;font-size:12px\">若非本人操作，请忽略此邮件。</p>"
                + "</div></div>";
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "***" + email.substring(at);
    }
}
