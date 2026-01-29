package com.yunlbd.flexboot4.service;

import com.yunlbd.flexboot4.entity.SysFileChunk;

import java.util.List;

public interface FileEmbeddingPublisher {
    /**
     * 发布单个 chunk 的 embedding 任务
     */
    void publishChunk(SysFileChunk chunk);

    /**
     * 批量发布多个 chunk 的 embedding 任务
     */
    void publishChunks(List<SysFileChunk> chunks);
}

