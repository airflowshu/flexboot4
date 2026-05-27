package com.yunlbd.flexboot4.cms;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CmsAutoConfigurationMetadataTest {

    @Test
    void cmsAutoConfigurationIsRegisteredForBootImports() throws IOException {
        try (var in = CmsAutoConfigurationMetadataTest.class.getResourceAsStream(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        )) {
            assertThat(in).isNotNull();
            String imports = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(imports).contains("com.yunlbd.flexboot4.autoconfigure.FlexBoot4CmsAutoConfiguration");
        }
    }
}
