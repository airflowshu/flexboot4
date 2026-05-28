package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.FileStorageProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileAccessTokenServiceTest {

    @Test
    void tokenShouldRoundTrip() {
        FileAccessTokenService service = service("secret-1234567890");
        Instant expireAt = Instant.now().plusSeconds(60);

        String token = service.createToken("file-1", expireAt, true);

        FileAccessTokenService.AccessToken parsed = service.verify(token);
        assertThat(parsed.fileId()).isEqualTo("file-1");
        assertThat(parsed.attachment()).isTrue();
        assertThat(parsed.expireAt().getEpochSecond()).isEqualTo(expireAt.getEpochSecond());
    }

    @Test
    void tokenShouldRejectTampering() {
        FileAccessTokenService service = service("secret-1234567890");
        String token = service.createToken("file-1", Instant.now().plusSeconds(60), false);

        assertThrows(IllegalArgumentException.class, () -> service.verify(token + "x"));
    }

    @Test
    void tokenShouldRejectExpiredToken() {
        FileAccessTokenService service = service("secret-1234567890");
        String token = service.createToken("file-1", Instant.now().minusSeconds(1), false);

        assertThrows(IllegalArgumentException.class, () -> service.verify(token));
    }

    private FileAccessTokenService service(String secret) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setAccessTokenSecret(secret);
        return new FileAccessTokenService(properties);
    }
}
