package com.yunlbd.flexboot4.task;

import com.yunlbd.flexboot4.service.ops.impl.AiApiKeyServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiApiKeySnapshotTask {

    private final AiApiKeyServiceImpl aiApiKeyService;

    @Scheduled(initialDelay = 15000, fixedDelay = 300000)
    public void refreshSnapshot() {
        aiApiKeyService.rebuildSnapshot();
    }
}

