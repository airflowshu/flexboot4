package com.yunlbd.flexboot4.service.kb;

import com.yunlbd.flexboot4.entity.kb.SysFileChunk;

import java.util.List;

public interface KbEmbeddingPublisher {

    void publishChunk(String kbId, SysFileChunk chunk);

    void publishChunks(String kbId, List<SysFileChunk> chunks);
}

