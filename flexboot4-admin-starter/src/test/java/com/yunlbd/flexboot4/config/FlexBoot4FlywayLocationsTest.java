package com.yunlbd.flexboot4.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class FlexBoot4FlywayLocationsTest {

    @Test
    void adminMigrationsShouldBeDisabledByDefault() {
        MockEnvironment environment = new MockEnvironment();

        assertThat(FlexBoot4FlywayLocations.adminMigrationsEnabled(environment)).isFalse();
    }

    @Test
    void mergedLocationsShouldAppendAdminLocationToDefaultFlywayLocation() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(FlexBoot4FlywayLocations.ENABLED_PROPERTY, "true");

        assertThat(FlexBoot4FlywayLocations.adminMigrationsEnabled(environment)).isTrue();
        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration,classpath:db/migration/flexboot4/admin/postgresql");
    }

    @Test
    void mergedLocationsShouldPreserveCustomLocationsAndAvoidDuplicates() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty(FlexBoot4FlywayLocations.LOCATIONS_PROPERTY,
                        "classpath:db/migration/custom, classpath:db/migration/flexboot4/admin/postgresql");

        assertThat(FlexBoot4FlywayLocations.mergedLocations(environment))
                .isEqualTo("classpath:db/migration/custom,classpath:db/migration/flexboot4/admin/postgresql");
    }
}
