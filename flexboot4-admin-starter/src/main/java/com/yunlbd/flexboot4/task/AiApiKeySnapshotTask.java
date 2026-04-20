package com.yunlbd.flexboot4.task;

import com.yunlbd.flexboot4.service.ops.AiApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "flexboot4.schedule", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiApiKeySnapshotTask {

    private final AiApiKeyService aiApiKeyService;

    @Scheduled(initialDelay = 15000, fixedDelay = 300000)
    public void refreshSnapshot() {
        aiApiKeyService.rebuildSnapshot();
    }
}

