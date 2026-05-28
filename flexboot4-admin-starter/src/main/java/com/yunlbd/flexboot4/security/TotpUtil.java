package com.yunlbd.flexboot4.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Locale;

@Component
public class TotpUtil {

    public static final String ISSUER = "FlexBoot4";
    public static final String ALGORITHM = "HmacSHA1";
    public static final int DIGITS = 6;
    public static final int PERIOD_SECONDS = 30;
    private static final int WINDOW_STEPS = 1;
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] BASE32_LOOKUP = new int[128];
    private static final int SECRET_BYTES = 20;

    static {
        for (int i = 0; i < BASE32_LOOKUP.length; i++) {
            BASE32_LOOKUP[i] = -1;
        }
        for (int i = 0; i < BASE32_ALPHABET.length; i++) {
            BASE32_LOOKUP[BASE32_ALPHABET[i]] = i;
        }
    }

    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    public TotpUtil() {
        this(Clock.systemUTC());
    }

    TotpUtil(Clock clock) {
        this.clock = clock;
    }

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public String buildOtpAuthUri(String secret, String accountName) {
        String account = ISSUER + ":" + accountName;
        return "otpauth://totp/"
                + urlEncode(account)
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1"
                + "&digits=" + DIGITS
                + "&period=" + PERIOD_SECONDS;
    }

    public boolean verify(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.trim().matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        long currentStep = clock.millis() / 1000 / PERIOD_SECONDS;
        String normalized = code.trim();
        for (long step = currentStep - WINDOW_STEPS; step <= currentStep + WINDOW_STEPS; step++) {
            if (generateCode(secret, step).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public String generateCode(String secret, long timeStep) {
        try {
            byte[] key = decodeBase32(secret);
            byte[] counter = ByteBuffer.allocate(Long.BYTES).putLong(timeStep).array();
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] hash = mac.doFinal(counter);
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate TOTP code", e);
        }
    }

    private static String encodeBase32(byte[] bytes) {
        StringBuilder result = new StringBuilder((bytes.length + 4) / 5 * 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1f]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET[(buffer << (5 - bitsLeft)) & 0x1f]);
        }
        return result.toString();
    }

    private static byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
        ByteBuffer output = ByteBuffer.allocate(normalized.length() * 5 / 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= BASE32_LOOKUP.length || BASE32_LOOKUP[c] < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer = (buffer << 5) | BASE32_LOOKUP[c];
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.put((byte) ((buffer >> (bitsLeft - 8)) & 0xff));
                bitsLeft -= 8;
            }
        }
        byte[] bytes = new byte[output.position()];
        output.flip();
        output.get(bytes);
        return bytes;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
