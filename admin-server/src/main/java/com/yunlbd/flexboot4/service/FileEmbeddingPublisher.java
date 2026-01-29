package com.yunlbd.flexboot4.service;

public interface FileEmbeddingPublisher {
    void publish(String fileId, int chunkCount, String embeddingModel);
}

