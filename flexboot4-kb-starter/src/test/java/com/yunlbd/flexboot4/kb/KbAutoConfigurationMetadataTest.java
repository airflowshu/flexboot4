package com.yunlbd.flexboot4.kb;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KbAutoConfigurationMetadataTest {

    @Test
    void kbAutoConfigurationIsRegisteredForBootImports() throws IOException {
        String imports = resourceAsString(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        );

        assertThat(imports).contains("com.yunlbd.flexboot4.autoconfigure.FlexBoot4KbAutoConfiguration");
    }

    @Test
    void legacyEnvironmentListenerRegistrationIsPreserved() throws IOException {
        String factories = resourceAsString("/META-INF/spring.factories");

        assertThat(factories)
                .contains("org.springframework.context.ApplicationListener")
                .contains("com.yunlbd.flexboot4.config.FlexBoot4KbEnvironmentListener");
    }

    @Test
    void embeddingStreamIsDisabledByDefault() throws IOException {
        String defaults = resourceAsString("/flexboot4-kb-defaults.yml");

        assertThat(defaults).contains("enabled: ${FILE_EMBEDDING_STREAM_ENABLED:false}");
    }

    @Test
    void kbDatabaseResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/db/init.sql"))
                .contains("CREATE TABLE IF NOT EXISTS knowledge_base")
                .contains("CREATE TABLE IF NOT EXISTS sys_file_chunk");
        assertThat(resourceAsString("/db/menu_data.sql"))
                .contains("kb_menu_root")
                .contains("kb:manage:list");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = KbAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
