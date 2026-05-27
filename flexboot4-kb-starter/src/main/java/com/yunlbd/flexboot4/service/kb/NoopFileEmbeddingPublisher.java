package com.yunlbd.flexboot4.service.kb;

import com.yunlbd.flexboot4.entity.kb.SysFileChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnMissingBean(FileEmbeddingPublisher.class)
public class NoopFileEmbeddingPublisher implements FileEmbeddingPublisher {

    @Override
    public void publishChunk(SysFileChunk chunk) {
        // Embedding stream is disabled.
    }

    @Override
    public void publishChunks(List<SysFileChunk> chunks) {
        // Embedding stream is disabled.
    }
}
