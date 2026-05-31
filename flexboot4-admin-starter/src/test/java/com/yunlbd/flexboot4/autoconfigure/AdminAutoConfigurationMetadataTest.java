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
    void adminFlywayModuleResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/META-INF/flexboot4/flyway-module.properties"))
                .contains("module=admin")
                .contains("locations=classpath:db/flexboot4-migration/admin/postgresql");
        assertThat(resourceAsString("/db/flexboot4-migration/admin/postgresql/V1000__admin_core_schema.sql"))
                .contains("CREATE TABLE IF NOT EXISTS sys_user")
                .contains("CREATE TABLE IF NOT EXISTS sys_oper_log");
        assertThat(resourceAsString("/db/flexboot4-migration/admin/postgresql/V1010__admin_menu_data.sql"))
                .contains("sys_menu_system")
                .contains("sys:user:reset-password");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = AdminAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
