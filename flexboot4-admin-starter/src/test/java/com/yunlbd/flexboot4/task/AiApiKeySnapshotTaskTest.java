package com.yunlbd.flexboot4.task;

import com.yunlbd.flexboot4.lock.DistributedLockService;
import com.yunlbd.flexboot4.service.ops.AiApiKeyService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiApiKeySnapshotTaskTest {

    private final AiApiKeyService aiApiKeyService = mock(AiApiKeyService.class);
    private final DistributedLockService distributedLockService = mock(DistributedLockService.class);
    private final AiApiKeySnapshotTask task = new AiApiKeySnapshotTask(aiApiKeyService, distributedLockService);

    @Test
    void refreshSnapshotShouldRunOnlyWhenLockAcquired() {
        doAnswer(invocation -> {
            invocation.getArgument(2, Runnable.class).run();
            return true;
        }).when(distributedLockService).executeIfLocked(eq("admin:ai-api-key:snapshot"), any(Duration.class), any(Runnable.class));

        task.refreshSnapshot();

        verify(aiApiKeyService).rebuildSnapshot();
    }

    @Test
    void refreshSnapshotShouldSkipWhenLockRejected() {
        when(distributedLockService.executeIfLocked(eq("admin:ai-api-key:snapshot"), any(Duration.class), any(Runnable.class)))
                .thenReturn(false);

        task.refreshSnapshot();

        verify(aiApiKeyService, never()).rebuildSnapshot();
    }
}
