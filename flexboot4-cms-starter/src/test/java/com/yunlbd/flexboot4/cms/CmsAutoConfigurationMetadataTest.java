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

    @Test
    void cmsFlywayModuleResourcesArePackaged() throws IOException {
        assertThat(resourceAsString("/META-INF/flexboot4/flyway-module.properties"))
                .contains("module=cms")
                .contains("locations=classpath:db/flexboot4-migration/cms/postgresql");
        assertThat(resourceAsString("/db/flexboot4-migration/cms/postgresql/V3000__cms_schema.sql"))
                .contains("CREATE TABLE IF NOT EXISTS cms_category")
                .contains("CREATE TABLE IF NOT EXISTS cms_template_publish_record");
        assertThat(resourceAsString("/db/flexboot4-migration/cms/postgresql/V3010__cms_menu_data.sql"))
                .contains("cms_menu_root")
                .contains("cms:article:list");
    }

    private static String resourceAsString(String path) throws IOException {
        try (var in = CmsAutoConfigurationMetadataTest.class.getResourceAsStream(path)) {
            assertThat(in).as("resource %s", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
