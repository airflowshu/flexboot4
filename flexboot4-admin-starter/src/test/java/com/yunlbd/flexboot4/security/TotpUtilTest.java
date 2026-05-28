package com.yunlbd.flexboot4.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TotpUtilTest {

    @Test
    void generatesRfc6238CompatibleTotpCode() {
        TotpUtil totpUtil = new TotpUtil(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));

        String code = totpUtil.generateCode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1);

        assertThat(code).isEqualTo("287082");
    }

    @Test
    void verifiesCurrentCodeWithinAllowedWindow() {
        TotpUtil totpUtil = new TotpUtil(Clock.fixed(Instant.ofEpochSecond(59), ZoneOffset.UTC));
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

        assertThat(totpUtil.verify(secret, "287082")).isTrue();
        assertThat(totpUtil.verify(secret, "000000")).isFalse();
    }

    @Test
    void buildsStandardOtpAuthUri() {
        TotpUtil totpUtil = new TotpUtil(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        String uri = totpUtil.buildOtpAuthUri("ABCDEF234567", "alice@A7K3P2");

        assertThat(uri)
                .startsWith("otpauth://totp/FlexBoot4%3Aalice%40A7K3P2")
                .contains("secret=ABCDEF234567")
                .contains("issuer=FlexBoot4")
                .contains("digits=6")
                .contains("period=30");
    }
}
