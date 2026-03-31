package com.yunlbd.flexboot4.media;

import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.media.core.MediaHookValidator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaHookValidatorTest {

    @Test
    void shouldValidateConfiguredSignature() throws Exception {
        MediaServer server = MediaServer.builder()
                .hookSecret("secret-1")
                .build();
        MediaProperties properties = new MediaProperties(
                true,
                "http://localhost:8080",
                "http-flv",
                "media_snapshot",
                2,
                8,
                1024,
                "X-Media-Hook-Signature",
                300,
                true,
                30000,
                30000,
                180,
                180,
                30,
                300,
                true
        );
        MediaHookValidator validator = new MediaHookValidator(properties);
        String body = "{\"stream\":\"camera-01\"}";
        String signature = sha256(body + ":" + server.getHookSecret());

        assertTrue(validator.validate(server, body, signature));
        assertFalse(validator.validate(server, body, "invalid"));
    }

    @Test
    void shouldValidateTimestampSignature() throws Exception {
        MediaServer server = MediaServer.builder()
                .hookSecret("secret-1")
                .build();
        MediaProperties properties = new MediaProperties(
                true,
                "http://localhost:8080",
                "http-flv",
                "media_snapshot",
                2,
                8,
                1024,
                "X-Media-Hook-Signature",
                300,
                true,
                30000,
                30000,
                180,
                180,
                30,
                300,
                true
        );
        MediaHookValidator validator = new MediaHookValidator(properties);
        String body = "{\"stream\":\"camera-01\"}";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = sha256(timestamp + ":" + body + ":" + server.getHookSecret());

        assertTrue(validator.validate(server, body, signature, timestamp));
        assertFalse(validator.validate(server, body, signature, String.valueOf(LocalDateTime.now().minusHours(2).toEpochSecond(java.time.ZoneOffset.UTC))));
        assertFalse(validator.validate(server, body, signature, "invalid-time"));
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
