package com.yunlbd.flexboot4.cache;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.autoconfigure.FlexBoot4KernelAutoConfiguration;
import com.yunlbd.flexboot4.common.annotation.BumpTableVersion;
import com.yunlbd.flexboot4.service.testsupport.TestWriteServices.ImplementationAnnotatedWriteService;
import com.yunlbd.flexboot4.service.testsupport.TestWriteServices.ImplementationAnnotatedWriteServiceImpl;
import com.yunlbd.flexboot4.service.testsupport.TestWriteServices.InterfaceAnnotatedWriteService;
import com.yunlbd.flexboot4.service.testsupport.TestWriteServices.InterfaceAnnotatedWriteServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class BumpTableVersionAspectTest {

    private final CountingTableVersionProvider provider = new CountingTableVersionProvider();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FlexBoot4KernelAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @BeforeEach
    void setUp() {
        TableVersions.setProvider(provider);
    }

    @AfterEach
    void tearDown() {
        TableVersions.setProvider(new TableVersionProvider() {
            @Override
            public long getVersion(String table) {
                return 0;
            }

            @Override
            public long bumpVersion(String table) {
                return 0;
            }
        });
    }

    @Test
    void bumpsEntityTableWhenBooleanResultIsTrue() {
        contextRunner.run(context -> {
            DemoWriteService service = context.getBean(DemoWriteService.class);

            assertThat(service.writeOk()).isTrue();
            assertThat(provider.getVersion("demo_table")).isEqualTo(1);

            assertThat(service.writeFailed()).isFalse();
            assertThat(provider.getVersion("demo_table")).isEqualTo(1);
        });
    }

    @Test
    void bumpsExplicitTablesWhenVoidMethodReturnsNormally() {
        contextRunner.run(context -> {
            DemoWriteService service = context.getBean(DemoWriteService.class);

            service.writeExplicitTable();

            assertThat(provider.getVersion("extra_table")).isEqualTo(1);
        });
    }

    @Test
    void supportsInterfaceProxyAnnotation() {
        contextRunner.run(context -> {
            InterfaceAnnotatedWriteService service = context.getBean(InterfaceAnnotatedWriteService.class);

            assertThat(service.writeFromInterfaceAnnotation()).isTrue();

            assertThat(provider.getVersion("interface_table")).isEqualTo(1);
        });
    }

    @Test
    void supportsImplementationAnnotationBehindInterfaceProxy() {
        contextRunner.run(context -> {
            ImplementationAnnotatedWriteService service = context.getBean(ImplementationAnnotatedWriteService.class);

            assertThat(service.writeFromImplementationAnnotation()).isTrue();

            assertThat(provider.getVersion("implementation_table")).isEqualTo(1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
        @Bean
        DemoWriteService demoWriteService() {
            return new DemoWriteService();
        }

        @Bean
        InterfaceAnnotatedWriteService interfaceAnnotatedWriteService() {
            return new InterfaceAnnotatedWriteServiceImpl();
        }

        @Bean
        ImplementationAnnotatedWriteService implementationAnnotatedWriteService() {
            return new ImplementationAnnotatedWriteServiceImpl();
        }
    }

    static class DemoWriteService {
        @BumpTableVersion(DemoEntity.class)
        public boolean writeOk() {
            return true;
        }

        @BumpTableVersion(DemoEntity.class)
        public boolean writeFailed() {
            return false;
        }

        @BumpTableVersion(tables = "extra_table")
        public void writeExplicitTable() {
        }
    }

    @Table("demo_table")
    static class DemoEntity {
    }

    static class CountingTableVersionProvider implements TableVersionProvider {
        private final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();

        @Override
        public long getVersion(String table) {
            return versions.computeIfAbsent(table, ignored -> new AtomicLong()).get();
        }

        @Override
        public long bumpVersion(String table) {
            return versions.computeIfAbsent(table, ignored -> new AtomicLong()).incrementAndGet();
        }
    }
}
