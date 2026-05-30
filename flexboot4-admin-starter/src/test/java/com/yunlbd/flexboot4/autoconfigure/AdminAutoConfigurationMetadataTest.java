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
    void adminInitResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/db/init.sql"))
                .contains("CREATE TABLE IF NOT EXISTS sys_user")
                .contains("CREATE TABLE IF NOT EXISTS sys_menu")
                .contains("CREATE TABLE IF NOT EXISTS sys_oper_log");
        assertThat(resourceAsString("/db/menu_data.sql"))
                .contains("sys_menu_system")
                .contains("sys:user:reset-password")
                .contains("sys:oper:log:list");
    }

    @Test
    void adminFlywayMigrationResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V1__admin_permission_p0_patch.sql"))
                .contains("sys:user:reset-password");
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V2__operlog_event_id_reliability.sql"))
                .contains("uk_sys_oper_log_event_id")
                .contains("pg_tables");
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V4__auth_sms_login_options.sql"))
                .contains("auth.login.options")
                .contains("uk_sys_user_phone_alive");
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V6__sys_user_mfa.sql"))
                .contains("sys_user_mfa")
                .contains("uk_sys_user_mfa_enabled_totp");
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V7__auth_sms_ip_rate_limit.sql"))
                .contains("auth.sms.ipHourlyLimit")
                .contains("auth.sms.ipDailyLimit");
        assertThat(resourceAsString("/db/migration/flexboot4/admin/postgresql/V8__vben_menu_contract_fix.sql"))
                .contains("BasicLayout")
                .contains("sys:user:reset-password");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = AdminAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
