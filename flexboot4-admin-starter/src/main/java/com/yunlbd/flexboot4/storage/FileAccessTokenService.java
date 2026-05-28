package com.yunlbd.flexboot4.storage;

import com.yunlbd.flexboot4.config.FileStorageProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class FileAccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final FileStorageProperties properties;

    public FileAccessTokenService(FileStorageProperties properties) {
        this.properties = properties;
    }

    public String createToken(String fileId, Instant expireAt, boolean attachment) {
        String payload = fileId + "." + expireAt.getEpochSecond() + "." + (attachment ? "1" : "0");
        String signature = sign(payload);
        return base64Url(payload) + "." + signature;
    }

    public AccessToken verify(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("访问令牌不能为空");
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot >= token.length() - 1) {
            throw new IllegalArgumentException("访问令牌格式错误");
        }
        String encodedPayload = token.substring(0, dot);
        String signature = token.substring(dot + 1);
        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), sign(payload).getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("访问令牌签名无效");
        }

        String[] parts = payload.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("访问令牌内容无效");
        }
        Instant expireAt = Instant.ofEpochSecond(Long.parseLong(parts[1]));
        if (Instant.now().isAfter(expireAt)) {
            throw new IllegalArgumentException("访问令牌已过期");
        }
        return new AccessToken(parts[0], expireAt, "1".equals(parts[2]));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign file access token", e);
        }
    }

    private String resolveSecret() {
        String secret = properties.getAccessTokenSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("flexboot4.file-storage.access-token-secret must not be blank");
        }
        return secret;
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public record AccessToken(String fileId, Instant expireAt, boolean attachment) {
    }
}
