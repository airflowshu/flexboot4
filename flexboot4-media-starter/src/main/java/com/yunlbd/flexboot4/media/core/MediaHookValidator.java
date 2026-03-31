package com.yunlbd.flexboot4.media.core;

import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.media.MediaProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

@Component
public class MediaHookValidator {

    private final MediaProperties mediaProperties;

    public MediaHookValidator(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    public boolean validate(MediaServer server, String body, String providedSignature) {
        return validate(server, body, providedSignature, null);
    }

    public boolean validate(MediaServer server, String body, String providedSignature, String timestamp) {
        if (server == null || server.getHookSecret() == null || server.getHookSecret().isBlank()) {
            return true;
        }
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        if (timestamp != null && !timestamp.isBlank()) {
            LocalDateTime timestampValue = parseTimestamp(timestamp);
            if (timestampValue == null) {
                return false;
            }
            if (mediaProperties.hookTimestampToleranceSeconds() > 0) {
                long diff = Math.abs(java.time.Duration.between(timestampValue, LocalDateTime.now()).getSeconds());
                if (diff > mediaProperties.hookTimestampToleranceSeconds()) {
                    return false;
                }
            }
            if (sha256Hex(timestamp + ":" + body + ":" + server.getHookSecret()).equalsIgnoreCase(providedSignature)) {
                return true;
            }
        }
        return sha256Hex(body + ":" + server.getHookSecret()).equalsIgnoreCase(providedSignature);
    }

    public String signatureHeaderName() {
        return mediaProperties.hookSecretHeader();
    }

    public String timestampHeaderName() {
        return "X-Media-Hook-Timestamp";
    }

    private LocalDateTime parseTimestamp(String value) {
        try {
            if (value.matches("^\\d{13}$")) {
                long epochMillis = Long.parseLong(value);
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            }
            if (value.matches("^\\d{10}$")) {
                long epochSeconds = Long.parseLong(value);
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
            }
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException | NumberFormatException e) {
            return null;
        }
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte current : hashed) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate hook signature", e);
        }
    }
}
