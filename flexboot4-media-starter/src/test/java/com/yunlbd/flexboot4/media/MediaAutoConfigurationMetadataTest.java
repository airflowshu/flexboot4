package com.yunlbd.flexboot4.media;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MediaAutoConfigurationMetadataTest {

    @Test
    void mediaAutoConfigurationIsRegisteredForBootImports() throws IOException {
        String imports = resourceAsString(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        );

        assertThat(imports).contains("com.yunlbd.flexboot4.media.MediaAutoConfiguration");
    }

    @Test
    void legacyEnvironmentListenerRegistrationIsPreserved() throws IOException {
        String factories = resourceAsString("/META-INF/spring.factories");

        assertThat(factories)
                .contains("org.springframework.context.ApplicationListener")
                .contains("com.yunlbd.flexboot4.config.FlexBoot4MediaEnvironmentListener");
    }

    @Test
    void mediaFlywayModuleResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/META-INF/flexboot4/flyway-module.properties"))
                .contains("module=media")
                .contains("locations=classpath:db/flexboot4-migration/media/postgresql");
        assertThat(resourceAsString("/db/flexboot4-migration/media/postgresql/V4000__media_schema.sql"))
                .contains("CREATE TABLE IF NOT EXISTS media_server")
                .contains("CREATE TABLE IF NOT EXISTS media_cascade_binding");
        assertThat(resourceAsString("/db/flexboot4-migration/media/postgresql/V4010__media_menu_data.sql"))
                .contains("media_root")
                .contains("media:server:list");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = MediaAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
