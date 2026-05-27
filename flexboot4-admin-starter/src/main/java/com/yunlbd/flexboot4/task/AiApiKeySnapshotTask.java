package com.yunlbd.flexboot4.task;

import com.yunlbd.flexboot4.lock.DistributedLockService;
import com.yunlbd.flexboot4.service.ops.AiApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(prefix = "flexboot4.schedule", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiApiKeySnapshotTask {

    private final AiApiKeyService aiApiKeyService;
    private final DistributedLockService distributedLockService;

    @Scheduled(initialDelay = 15000, fixedDelay = 300000)
    public void refreshSnapshot() {
        distributedLockService.executeIfLocked("admin:ai-api-key:snapshot", Duration.ofMinutes(10), this::doRefreshSnapshot);
    }

    void doRefreshSnapshot() {
        aiApiKeyService.rebuildSnapshot();
    }
}

