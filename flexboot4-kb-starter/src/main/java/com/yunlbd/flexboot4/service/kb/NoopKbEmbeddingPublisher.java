package com.yunlbd.flexboot4.service.kb;

import com.yunlbd.flexboot4.entity.kb.SysFileChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnMissingBean(KbEmbeddingPublisher.class)
public class NoopKbEmbeddingPublisher implements KbEmbeddingPublisher {

    @Override
    public void publishChunk(String kbId, SysFileChunk chunk) {
        // Embedding stream is disabled.
    }

    @Override
    public void publishChunks(String kbId, List<SysFileChunk> chunks) {
        // Embedding stream is disabled.
    }
}
