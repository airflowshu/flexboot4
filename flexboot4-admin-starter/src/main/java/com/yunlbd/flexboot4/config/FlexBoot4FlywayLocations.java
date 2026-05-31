package com.yunlbd.flexboot4.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FlexBoot4FlywayLocations {

    static final String ENABLED_PROPERTY = "flexboot4.flyway.enabled";
    static final String DATABASE_PROPERTY = "flexboot4.flyway.database";
    static final String AUTO_DETECT_MODULES_PROPERTY = "flexboot4.flyway.auto-detect-modules";
    static final String MODULE_ENABLED_PROPERTY_PREFIX = "flexboot4.flyway.modules.";
    static final String MODULE_ENABLED_PROPERTY_SUFFIX = ".enabled";
    static final String LOCATIONS_PROPERTY = "spring.flyway.locations";
    static final String DEFAULT_FLYWAY_LOCATION = "classpath:db/migration";
    static final String MODULE_DESCRIPTOR_PATTERN = "classpath*:META-INF/flexboot4/flyway-module.properties";
    static final String DEFAULT_DATABASE = "postgresql";
    private static final Pattern VERSIONED_MIGRATION_PATTERN = Pattern.compile(".*/V(\\d+)__.*\\.sql");

    private FlexBoot4FlywayLocations() {
    }

    static boolean flexBoot4FlywayEnabled(ConfigurableEnvironment environment) {
        return environment.getProperty(ENABLED_PROPERTY, Boolean.class, true);
    }

    static boolean autoDetectModulesEnabled(ConfigurableEnvironment environment) {
        return environment.getProperty(AUTO_DETECT_MODULES_PROPERTY, Boolean.class, true);
    }

    static String mergedLocations(ConfigurableEnvironment environment) {
        Set<String> locations = configuredLocations(environment);
        discoverModules(environment).stream()
                .flatMap(module -> module.locations().stream())
                .forEach(locations::add);
        return String.join(",", locations);
    }

    static List<FlywayModule> discoverModules(ConfigurableEnvironment environment) {
        if (!flexBoot4FlywayEnabled(environment) || !autoDetectModulesEnabled(environment)) {
            return List.of();
        }
        String database = environment.getProperty(DATABASE_PROPERTY, DEFAULT_DATABASE);
        List<FlywayModule> modules = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            for (Resource resource : resolver.getResources(MODULE_DESCRIPTOR_PATTERN)) {
                FlywayModule.from(resource)
                        .filter(module -> module.matchesDatabase(database))
                        .filter(module -> moduleEnabled(environment, module.module()))
                        .ifPresent(modules::add);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to discover FlexBoot4 Flyway module descriptors", e);
        }
        return modules.stream()
                .map(FlexBoot4FlywayLocations::withFirstMigrationVersion)
                .sorted()
                .toList();
    }

    private static FlywayModule withFirstMigrationVersion(FlywayModule module) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        OptionalInt firstVersion = module.locations().stream()
                .map(FlexBoot4FlywayLocations::locationPattern)
                .flatMap(pattern -> resources(resolver, pattern).stream())
                .map(Resource::getDescription)
                .mapToInt(FlexBoot4FlywayLocations::migrationVersion)
                .filter(version -> version >= 0)
                .min();
        return firstVersion.isPresent()
                ? module.withFirstMigrationVersion(firstVersion.getAsInt())
                : module;
    }

    private static String locationPattern(String location) {
        return location.endsWith("/") ? location + "V*__*.sql" : location + "/V*__*.sql";
    }

    private static List<Resource> resources(PathMatchingResourcePatternResolver resolver, String pattern) {
        try {
            return Arrays.asList(resolver.getResources(pattern));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect Flyway migrations under " + pattern, e);
        }
    }

    private static int migrationVersion(String description) {
        Matcher matcher = VERSIONED_MIGRATION_PATTERN.matcher(description.replace('\\', '/'));
        if (!matcher.find()) {
            return -1;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static Set<String> configuredLocations(ConfigurableEnvironment environment) {
        Set<String> locations = new LinkedHashSet<>();
        Binder.get(environment)
                .bind(LOCATIONS_PROPERTY, Bindable.listOf(String.class))
                .orElseGet(List::of)
                .stream()
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(locations::add);
        if (locations.isEmpty()) {
            locations.add(DEFAULT_FLYWAY_LOCATION);
        }
        return locations;
    }

    private static boolean moduleEnabled(ConfigurableEnvironment environment, String module) {
        return environment.getProperty(
                MODULE_ENABLED_PROPERTY_PREFIX + module + MODULE_ENABLED_PROPERTY_SUFFIX,
                Boolean.class,
                true
        );
    }

    record FlywayModule(
            String module,
            String database,
            List<String> locations,
            int firstMigrationVersion
    ) implements Comparable<FlywayModule> {

        static java.util.Optional<FlywayModule> from(Resource resource) throws IOException {
            Properties properties = new Properties();
            try (InputStream inputStream = resource.getInputStream()) {
                properties.load(inputStream);
            }
            String module = trimToNull(properties.getProperty("module"));
            String database = trimToNull(properties.getProperty("database"));
            List<String> locations = splitLocations(properties.getProperty("locations"));
            if (module == null || database == null || locations.isEmpty()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new FlywayModule(module, database, locations, Integer.MAX_VALUE));
        }

        boolean matchesDatabase(String targetDatabase) {
            return database.equalsIgnoreCase(targetDatabase);
        }

        @Override
        public int compareTo(FlywayModule other) {
            int versionCompare = Integer.compare(this.firstMigrationVersion, other.firstMigrationVersion);
            if (versionCompare != 0) {
                return versionCompare;
            }
            return this.module.compareTo(other.module);
        }

        FlywayModule withFirstMigrationVersion(int firstMigrationVersion) {
            return new FlywayModule(module, database, locations, firstMigrationVersion);
        }

        private static List<String> splitLocations(String value) {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(location -> !location.isBlank())
                    .toList();
        }

        private static String trimToNull(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return value.trim();
        }
    }
}
