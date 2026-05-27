package com.yunlbd.flexboot4.lock;

import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisDistributedLockServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private final RedisDistributedLockService lockService = new RedisDistributedLockService(
            redisTemplate,
            new LockProperties("test:lock:", 60000),
            metricsRecorder
    );

    RedisDistributedLockServiceTest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void tryLockShouldUseSetIfAbsentWithPrefixAndTtl() {
        when(valueOperations.setIfAbsent(eq("test:lock:job"), any(String.class), eq(Duration.ofSeconds(5))))
                .thenReturn(true);

        assertThat(lockService.tryLock("job", Duration.ofSeconds(5))).isTrue();
        verify(metricsRecorder).increment(eq("flexboot4.lock.acquire"), any());
    }

    @Test
    void tryLockShouldReturnFalseWhenRedisLockExists() {
        when(valueOperations.setIfAbsent(eq("test:lock:job"), any(String.class), any(Duration.class)))
                .thenReturn(false);

        assertThat(lockService.tryLock("job", Duration.ofSeconds(5))).isFalse();
        verify(metricsRecorder).increment(eq("flexboot4.lock.acquire"), any());
    }

    @Test
    void unlockShouldDeleteOnlyOwnedLock() {
        when(valueOperations.setIfAbsent(eq("test:lock:job"), any(String.class), any(Duration.class)))
                .thenReturn(true);
        lockService.tryLock("job", Duration.ofSeconds(5));

        when(valueOperations.get("test:lock:job")).thenReturn(captureOwner());

        lockService.unlock("job");

        verify(redisTemplate).delete("test:lock:job");
    }

    @Test
    void unlockShouldKeepForeignLock() {
        when(valueOperations.get("test:lock:job")).thenReturn("other-owner");

        lockService.unlock("job");

        verify(redisTemplate, never()).delete("test:lock:job");
    }

    private String captureOwner() {
        return org.mockito.Mockito.mockingDetails(valueOperations)
                .getInvocations()
                .stream()
                .filter(invocation -> "setIfAbsent".equals(invocation.getMethod().getName()))
                .map(invocation -> invocation.getArgument(1, String.class))
                .findFirst()
                .orElseThrow();
    }
}
