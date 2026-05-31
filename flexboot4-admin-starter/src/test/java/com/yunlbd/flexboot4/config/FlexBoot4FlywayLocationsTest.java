package com.yunlbd.flexboot4.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class FlexBoot4FlywayLocationsTest {

    @Test
    void flexBoot4FlywayShouldBeEnabledByDefault() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(FlexBoot4FlywayLocations.flexBoot4FlywayEnabled(environment)).isTrue();
        assertThat(FlexBoot4FlywayLocations.autoDetectModulesEnabled(environment)).isTrue();
    }

    @Test
    void mergedLocationsShouldAppendDetectedAdminLocationToDefaultFlywayLocation() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration,classpath:db/flexboot4-migration/admin/postgresql");
    }

    @Test
    void mergedLocationsShouldPreserveCustomLocationsAndAvoidDuplicates() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(FlexBoot4FlywayLocations.LOCATIONS_PROPERTY,
                        "classpath:db/migration/custom, classpath:db/flexboot4-migration/admin/postgresql");

        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration/custom,classpath:db/flexboot4-migration/admin/postgresql");
    }

    @Test
    void mergedLocationsShouldPreserveListStyleCustomLocations() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(FlexBoot4FlywayLocations.LOCATIONS_PROPERTY + "[0]", "classpath:db/migration/custom")
                .withProperty(FlexBoot4FlywayLocations.LOCATIONS_PROPERTY + "[1]", "classpath:tenant/migration");

        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration/custom,classpath:tenant/migration,classpath:db/flexboot4-migration/admin/postgresql");
    }

    @Test
    void moduleCanBeDisabledByProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("flexboot4.flyway.modules.admin.enabled", "false");

        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration");
    }

    @Test
    void databaseMismatchShouldSkipModule() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(FlexBoot4FlywayLocations.DATABASE_PROPERTY, "mysql");

        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration");
    }

    @Test
    void discoveredModulesShouldBeOrderedByFirstMigrationVersion() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(FlexBoot4FlywayLocations.discoverModules(environment))
                .extracting(FlexBoot4FlywayLocations.FlywayModule::module)
                .containsExactly("admin");
    }
}
