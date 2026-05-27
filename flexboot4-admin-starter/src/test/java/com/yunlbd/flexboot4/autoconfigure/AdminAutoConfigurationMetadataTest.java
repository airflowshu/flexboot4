package com.yunlbd.flexboot4.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAutoConfigurationMetadataTest {

    @Test
    void adminAutoConfigurationIsRegisteredForBootImports() throws IOException {
        String imports = resourceAsString(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        );

        assertThat(imports)
                .contains("com.yunlbd.flexboot4.autoconfigure.FlexBoot4AdminAutoConfiguration");
    }

    @Test
    void legacyEnvironmentListenerRegistrationIsPreserved() throws IOException {
        String factories = resourceAsString("/META-INF/spring.factories");

        assertThat(factories)
                .contains("org.springframework.context.ApplicationListener")
                .contains("com.yunlbd.flexboot4.config.FlexBoot4AdminEnvironmentListener");
    }

    @Test
    void adminFlywayMigrationResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V1__admin_permission_p0_patch.sql"))
                .contains("sys:user:reset-password");
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V2__operlog_event_id_reliability.sql"))
                .contains("uk_sys_oper_log_event_id");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = AdminAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
