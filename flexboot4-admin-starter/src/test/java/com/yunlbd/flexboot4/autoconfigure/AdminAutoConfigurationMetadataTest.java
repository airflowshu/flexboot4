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
                .contains("CREATE TABLE IF NOT EXISTS sys_oper_log")
                .contains("current_quarter_table TEXT")
                .contains("CREATE TABLE IF NOT EXISTS %I (LIKE sys_oper_log INCLUDING ALL)");
        String adminMenuData = resourceAsString("/db/flexboot4-migration/admin/postgresql/V1010__admin_menu_data.sql");
        assertThat(adminMenuData)
                .contains("sys_menu_system")
                .contains("('sys_menu_file', 'sys_menu_devops', '/devops/file', 'DevopsFile', '/devops/file/file-manage'")
                .contains("('sys_menu_dict', 'sys_menu_system', '/system/dict', 'SystemDict', '/system/dict/index'")
                .contains("'system.dict.title'")
                .contains("('sys_menu_dict_type', 'sys_menu_dict', '', 'SystemDictType', '', 'system.dict.type'")
                .contains("'sys:dict:type:list'")
                .contains("('sys_dict_type_add_btn', 'sys_menu_dict_type'")
                .contains("('sys_menu_dict_item', 'sys_menu_dict', '', 'SystemDictItem', '', 'system.dict.item'")
                .contains("('sys_dict_item_add_btn', 'sys_menu_dict_item'")
                .contains("'sys:dict:item:list'")
                .contains("('sys_user_add_btn', 'sys_menu_user'")
                .contains("'sys:user:add'")
                .contains("('sys_role_add_btn', 'sys_menu_role'")
                .contains("'sys:role:add'")
                .contains("('sys_config_add_btn', 'sys_menu_config'")
                .contains("'sys:config:add'")
                .contains("SELECT 'sys_role_admin_menu_' || m.id, '2', m.id")
                .doesNotContain("/system/file")
                .doesNotContain("/system/dict-type")
                .doesNotContain("/system/dict-item")
                .contains("sys:user:reset-password");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = AdminAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
