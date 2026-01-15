package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Email service for sending system notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String mailUsername;

    private static final String RESET_TOKEN_KEY_PREFIX = "auth:reset-token:";
    private static final String RESET_TOKEN_EMAIL_KEY_PREFIX = "auth:reset-email:";

    /**
     * Send password reset email with reset link
     *
     * @param email  recipient email address
     * @param token  reset token
     * @param userId user ID for logging
     */
    public void sendPasswordResetEmail(String email, String token, String userId) {
        // Store token in Redis for validation
        String tokenKey = RESET_TOKEN_KEY_PREFIX + token;
        String emailKey = RESET_TOKEN_EMAIL_KEY_PREFIX + token;
        int expirationMinutes = mailProperties.getTokenExpirationMinutes();

        redisTemplate.opsForValue().set(tokenKey, userId, expirationMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(emailKey, email, expirationMinutes, TimeUnit.MINUTES);

        String resetLink = mailProperties.getResetUrl() + "?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", mailProperties.getSenderName(), mailUsername));
            helper.setTo(email);
            helper.setSubject("重置您的密码 - FlexBoot4");

            String htmlContent = buildResetEmailContent(resetLink, expirationMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {} for user: {}", email, userId);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send reset email", e);
        }
    }

    /**
     * Validate reset token and return associated user ID
     *
     * @param token reset token
     * @return user ID if valid, null otherwise
     */
    public String validateResetToken(String token) {
        String tokenKey = RESET_TOKEN_KEY_PREFIX + token;
        return redisTemplate.opsForValue().get(tokenKey);
    }

    /**
     * Validate reset token and email match
     *
     * @param token reset token
     * @param email expected email
     * @return true if valid
     */
    public boolean validateResetTokenWithEmail(String token, String email) {
        String tokenKey = RESET_TOKEN_KEY_PREFIX + token;
        String emailKey = RESET_TOKEN_EMAIL_KEY_PREFIX + token;

        String storedUserId = redisTemplate.opsForValue().get(tokenKey);
        String storedEmail = redisTemplate.opsForValue().get(emailKey);

        if (storedUserId == null || storedEmail == null) {
            return false;
        }

        return storedEmail.equals(email);
    }

    /**
     * Invalidate reset token after use
     *
     * @param token reset token
     */
    public void invalidateResetToken(String token) {
        String tokenKey = RESET_TOKEN_KEY_PREFIX + token;
        String emailKey = RESET_TOKEN_EMAIL_KEY_PREFIX + token;
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(emailKey);
    }

    /**
     * Send password reset notification email (admin reset)
     *
     * @param email       recipient email address
     * @param newPassword new password
     */
    public void sendPasswordResetNotification(String email, String newPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", mailProperties.getSenderName(), mailUsername));
            helper.setTo(email);
            helper.setSubject("您的密码已重置 - FlexBoot4");

            String htmlContent = buildNotificationEmailContent(newPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset notification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send password reset notification email to: {}", email, e);
            throw new RuntimeException("Failed to send notification email", e);
        }
    }

    private String buildNotificationEmailContent(String newPassword) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #dc2626;">您的密码已修改</h2>
                    <p>您的账户密码已被管理员重置。以下是您的新密码：</p>
                    <div style="background-color: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center;">
                        <span style="font-size: 24px; font-weight: bold; color: #2563eb;">%s</span>
                    </div>
                    <p style="color: #dc2626; font-weight: bold;">出于安全考虑，建议您登录后立即修改密码。</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="color: #999; font-size: 12px;">如果您未进行此操作，请立即联系系统管理员。</p>
                </div>
            </body>
            </html>
            """.formatted(newPassword);
    }

    private String buildResetEmailContent(String resetLink, int expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2563eb;">重置您的密码</h2>
                    <p>您收到此邮件是因为您请求重置密码。请点击下方链接重置密码：</p>
                    <p style="margin: 30px 0;">
                        <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;">
                            重置密码
                        </a>
                    </p>
                    <p>或者复制以下链接到浏览器：</p>
                    <p style="word-break: break-all; color: #666; font-size: 14px;">%s</p>
                    <p style="color: #666; font-size: 14px;">此链接将在 %d 分钟后失效。</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="color: #999; font-size: 12px;">如果您没有请求重置密码，请忽略此邮件。</p>
                </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink, expirationMinutes);
    }
}
