package com.yunlbd.flexboot4.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MfaSecretCipherTest {

    @Test
    void encryptsAndDecryptsSecret() {
        MfaSecretCipher cipher = new MfaSecretCipher("test-secret-key-with-enough-entropy");

        String encrypted = cipher.encrypt("ABCDEF234567");

        assertThat(encrypted).isNotEqualTo("ABCDEF234567");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("ABCDEF234567");
    }
}
