package com.yunlbd.flexboot4.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class StarterArchitectureRulesTest {

    private static final Path ROOT = findRoot();
    private static final List<String> PRODUCTION_SOURCE_DIRS = List.of(
            "flexboot4-admin-kernel/src/main/java",
            "flexboot4-admin-starter/src/main/java",
            "flexboot4-kb-starter/src/main/java",
            "flexboot4-media-starter/src/main/java",
            "flexboot4-sms4j-starter/src/main/java",
            "flexboot4-cms-starter/src/main/java"
    );
    private static final List<String> STARTER_BUILD_FILES = List.of(
            "flexboot4-admin-kernel/build.gradle.kts",
            "flexboot4-admin-starter/build.gradle.kts",
            "flexboot4-kb-starter/build.gradle.kts",
            "flexboot4-media-starter/build.gradle.kts",
            "flexboot4-sms4j-starter/build.gradle.kts",
            "flexboot4-cms-starter/build.gradle.kts"
    );
    private static final Map<String, List<String>> FORBIDDEN_MODULE_DEPENDENCIES = Map.of(
            "flexboot4-admin-kernel/build.gradle.kts", List.of("project(\":flexboot4-admin-starter\")"),
            "flexboot4-kb-starter/build.gradle.kts", List.of("project(\":flexboot4-admin-starter\")"),
            "flexboot4-media-starter/build.gradle.kts", List.of("project(\":flexboot4-admin-starter\")"),
            "flexboot4-sms4j-starter/build.gradle.kts", List.of("project(\":flexboot4-admin-starter\")"),
            "flexboot4-cms-starter/build.gradle.kts", List.of("project(\":flexboot4-admin-starter\")")
    );
    private static final Pattern IMPL_FIELD_INJECTION = Pattern.compile(
            ".*private\\s+(?:final\\s+)?[\\w<>?,\\s]+Impl\\s+\\w+\\s*;.*"
    );
    private static final Pattern IMPL_CONSTRUCTOR_PARAMETER = Pattern.compile(
            ".*public\\s+\\w+\\s*\\([^)]*\\w+Impl\\s+\\w+.*"
    );

    @Test
    void productionCodeDoesNotUseAutowiredInjection() throws IOException {
        List<String> violations = javaFiles().stream()
                .filter(file -> contains(file, "@Autowired"))
                .map(StarterArchitectureRulesTest::relative)
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void productionCodeDoesNotInjectConcreteImplTypes() throws IOException {
        List<String> violations = javaFiles().stream()
                .filter(file -> {
                    String source = read(file);
                    return source.lines().anyMatch(line ->
                            IMPL_FIELD_INJECTION.matcher(line).matches()
                                    || IMPL_CONSTRUCTOR_PARAMETER.matcher(line).matches());
                })
                .map(StarterArchitectureRulesTest::relative)
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void productionCodeDoesNotImportFeatureImplPackagesAcrossBoundaries() throws IOException {
        List<String> violations = javaFiles().stream()
                .filter(file -> {
                    String path = relative(file);
                    String source = read(file);
                    return source.lines().anyMatch(line -> isForbiddenImplImport(path, line));
                })
                .map(StarterArchitectureRulesTest::relative)
                .toList();

        assertThat(violations).isEmpty();
    }

    @Test
    void moduleDependenciesFollowKernelBoundary() {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : FORBIDDEN_MODULE_DEPENDENCIES.entrySet()) {
            Path buildFile = ROOT.resolve(entry.getKey());
            String source = read(buildFile);
            for (String dependency : entry.getValue()) {
                if (source.contains(dependency)) {
                    violations.add(entry.getKey() + " must not depend on " + dependency);
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    private static List<Path> javaFiles() throws IOException {
        try (var paths = PRODUCTION_SOURCE_DIRS.stream()
                .map(ROOT::resolve)
                .filter(Files::exists)
                .flatMap(dir -> {
                    try {
                        return Files.walk(dir);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                })) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    private static boolean contains(Path file, String needle) {
        return read(file).contains(needle);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String relative(Path path) {
        return ROOT.relativize(path).toString().replace('\\', '/');
    }

    private static Path findRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate repository root from current working directory");
    }

    private static boolean isForbiddenImplImport(String path, String line) {
        if (!line.startsWith("import com.yunlbd.flexboot4.") || !line.contains(".impl.")) {
            return false;
        }
        if (line.contains("com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl")) {
            return false;
        }
        String currentModule = path.substring(0, path.indexOf('/'));
        if (line.contains("com.yunlbd.flexboot4.service.")) {
            String importedFeature = line.replace("import com.yunlbd.flexboot4.service.", "");
            String feature = importedFeature.substring(0, importedFeature.indexOf('.'));
            return !currentModule.contains(feature) && !("sys".equals(feature) && currentModule.contains("admin"));
        }
        return true;
    }
}
