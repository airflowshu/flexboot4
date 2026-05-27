package com.yunlbd.flexboot4.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KernelAutoConfigurationMetadataTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlexBoot4KernelAutoConfiguration.class));

    @Test
    void kernelAutoConfigurationIsRegisteredForBootImports() throws IOException {
        try (var in = KernelAutoConfigurationMetadataTest.class.getResourceAsStream(
                "/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
        )) {
            assertThat(in).isNotNull();
            String imports = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(imports)
                    .contains("com.yunlbd.flexboot4.autoconfigure.FlexBoot4KernelAutoConfiguration");
        }
    }

    @Test
    void kernelAutoConfigurationDoesNotEnableAdminSecurityBeans() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean("permissionCheckInterceptor")
                .doesNotHaveBean("securityConfig"));
    }
}
