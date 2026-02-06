package com.yunlbd.flexboot4.file;

public record FileObject(
        String id,
        String tenantId,
        String bizType,
        String bizId,
        String fileName,
        String fileExt,
        String mimeType,
        long fileSize,
        String fileHash,
        FileLocation location,
        String aiStatus,
        int chunkCount,
        Integer tokenEstimate,
        String embeddingModel
) {
}
