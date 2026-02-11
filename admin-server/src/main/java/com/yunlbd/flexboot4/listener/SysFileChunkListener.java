package com.yunlbd.flexboot4.listener;

import com.yunlbd.flexboot4.event.SysFileParsedEvent;
import com.yunlbd.flexboot4.service.kb.FileChunkingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SysFileChunkListener {

    private final FileChunkingService fileChunkingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFileParsed(SysFileParsedEvent event) {
        if (event == null || event.fileId() == null || event.fileId().isBlank()) {
            return;
        }
        fileChunkingService.chunk(event.fileId());
    }
}
