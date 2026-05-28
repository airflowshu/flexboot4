package com.yunlbd.flexboot4.service.sys;

import com.yunlbd.flexboot4.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnClass(JavaMailSender.class)
@ConditionalOnProperty(prefix = "flexboot4.mail", name = "enabled", havingValue = "true")
public class EmailService {

    private static final String RESET_TOKEN_KEY_PREFIX = "auth:reset-token:";
    private static final String RESET_TOKEN_EMAIL_KEY_PREFIX = "auth:reset-email:";

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String mailUsername;

    /**
     * Send password reset email with one-time reset link.
     */
    public void sendPasswordResetEmail(String email, String token, String userId) {
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
            helper.setSubject("Reset your password - FlexBoot4");
            helper.setText(buildResetEmailContent(resetLink, expirationMinutes), true);

            mailSender.send(message);
            log.info("Password reset email sent to: {} for user: {}", email, userId);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send reset email", e);
        }
    }

    /**
     * Send a verification code email for account security flows.
     */
    public void sendVerificationCodeEmail(String email, String code, int expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(String.format("%s <%s>", mailProperties.getSenderName(), mailUsername));
            helper.setTo(email);
            helper.setSubject("Security email verification code - FlexBoot4");
            helper.setText(buildVerificationCodeEmailContent(code, expirationMinutes), true);

            mailSender.send(message);
            log.info("Security email verification code sent to: {}", maskEmail(email));
        } catch (MessagingException e) {
            log.error("Failed to send security email verification code to: {}", maskEmail(email), e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Validate reset token and return associated user ID.
     */
    public String validateResetToken(String token) {
        String tokenKey = RESET_TOKEN_KEY_PREFIX + token;
        return redisTemplate.opsForValue().get(tokenKey);
    }

    /**
     * Validate reset token and email match.
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
     * Invalidate reset token after use.
     */
    public void invalidateResetToken(String token) {
        String tokenKey = RESET_TOKEN_KEY_PREFIX + token;
        String emailKey = RESET_TOKEN_EMAIL_KEY_PREFIX + token;
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(emailKey);
    }

    private String buildResetEmailContent(String resetLink, int expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset=\"UTF-8\">
            </head>
            <body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">
                <div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">
                    <h2 style=\"color: #2563eb;\">Reset your password</h2>
                    <p>We received a request to reset your password. Click the link below to continue:</p>
                    <p style=\"margin: 30px 0;\">
                        <a href=\"%s\" style=\"background-color: #2563eb; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block;\">
                            Reset Password
                        </a>
                    </p>
                    <p>Or copy this URL into your browser:</p>
                    <p style=\"word-break: break-all; color: #666; font-size: 14px;\">%s</p>
                    <p style=\"color: #666; font-size: 14px;\">This link expires in %d minutes.</p>
                    <hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\">
                    <p style=\"color: #999; font-size: 12px;\">If you did not request this, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(resetLink, resetLink, expirationMinutes);
    }

    private String buildVerificationCodeEmailContent(String code, int expirationMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset=\"UTF-8\">
            </head>
            <body style=\"font-family: Arial, sans-serif; line-height: 1.6; color: #333;\">
                <div style=\"max-width: 600px; margin: 0 auto; padding: 20px;\">
                    <h2 style=\"color: #2563eb;\">Verify your security email</h2>
                    <p>Use the verification code below to bind or change your backup email:</p>
                    <p style=\"font-size: 28px; font-weight: 700; letter-spacing: 4px; color: #111827; margin: 24px 0;\">%s</p>
                    <p style=\"color: #666; font-size: 14px;\">This code expires in %d minutes.</p>
                    <hr style=\"border: none; border-top: 1px solid #eee; margin: 30px 0;\">
                    <p style=\"color: #999; font-size: 12px;\">If you did not request this, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(code, expirationMinutes);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        int visibleLength = at > 3 ? 3 : 1;
        return email.substring(0, visibleLength) + "***" + email.substring(at);
    }
}
