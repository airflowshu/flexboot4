package com.yunlbd.flexboot4.config;

import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

final class FlexBoot4FlywayLocations {

    static final String ENABLED_PROPERTY = "flexboot4.flyway.admin-migrations-enabled";
    static final String LOCATIONS_PROPERTY = "spring.flyway.locations";
    static final String DEFAULT_FLYWAY_LOCATION = "classpath:db/migration";
    static final String ADMIN_POSTGRESQL_LOCATION = "classpath:db/migration/flexboot4/admin/postgresql";

    private FlexBoot4FlywayLocations() {
    }

    static boolean adminMigrationsEnabled(ConfigurableEnvironment environment) {
        return environment.getProperty(ENABLED_PROPERTY, Boolean.class, false);
    }

    static String mergedLocations(ConfigurableEnvironment environment) {
        String configured = environment.getProperty(LOCATIONS_PROPERTY);
        Set<String> locations = new LinkedHashSet<>();
        if (configured == null || configured.isBlank()) {
            locations.add(DEFAULT_FLYWAY_LOCATION);
        } else {
            Arrays.stream(configured.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(locations::add);
        }
        locations.add(ADMIN_POSTGRESQL_LOCATION);
        return String.join(",", locations);
    }
}
