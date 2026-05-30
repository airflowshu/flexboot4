package com.yunlbd.flexboot4.autoconfigure;

import com.yunlbd.flexboot4.auth.CurrentUserAutoConfiguration;
import com.yunlbd.flexboot4.lock.DistributedLockAutoConfiguration;
import com.yunlbd.flexboot4.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Kernel auto-configuration entry.
 *
 * <p>The kernel module currently exposes shared base classes and utilities.
 * It intentionally does not component-scan the shared root package, so pulling
 * in {@code admin-kernel} alone cannot accidentally enable admin RBAC beans.</p>
 */
@AutoConfiguration
@Import({
        CurrentUserAutoConfiguration.class,
        DistributedLockAutoConfiguration.class,
        MetricsAutoConfiguration.class,
        TableVersionAutoConfiguration.class
})
public class FlexBoot4KernelAutoConfiguration {
}
