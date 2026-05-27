package com.yunlbd.flexboot4.media;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MediaDefaultsTest {

    @Test
    void mediaRuntimeIsDisabledByDefault() throws IOException {
        try (var in = MediaDefaultsTest.class.getResourceAsStream("/flexboot4-media-defaults.yml")) {
            assertThat(in).isNotNull();
            String defaults = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(defaults)
                    .contains("enabled: ${MEDIA_ENABLED:false}")
                    .contains("runtime-check-enabled: ${MEDIA_RUNTIME_CHECK_ENABLED:false}");
        }
    }
}
