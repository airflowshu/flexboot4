package com.yunlbd.flexboot4.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /**
     * SMTP server host
     */
    private String host;

    /**
     * SMTP server port
     */
    private int port = 587;

    /**
     * Email username
     */
    private String username;

    /**
     * Email password or app password
     */
    private String password;

    /**
     * Sender name displayed in emails
     */
    private String senderName = "FlexBoot4";

    /**
     * Reset link base URL (frontend)
     */
    private String resetUrl = "http://localhost:5666/auth/forget-password";

    /**
     * Reset token expiration time in minutes
     */
    private int tokenExpirationMinutes = 30;
}
