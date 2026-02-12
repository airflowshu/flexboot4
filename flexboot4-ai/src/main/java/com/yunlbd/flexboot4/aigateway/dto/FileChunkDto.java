package com.yunlbd.flexboot4.aigateway.dto;

/**
 * 文件分片 DTO (从 admin 库的 sys_file_chunk 表读取)
 */
public class FileChunkDto {

    private String id;
    private String fileId;
    private Integer chunkIndex;
    private String content;
    private String contentHash;
    private String embeddingModel;
    private Integer tokenCount;
    private String embedStatus;

    public FileChunkDto() {
    }

    public FileChunkDto(String id, String fileId, Integer chunkIndex, String content,
                        String contentHash, String embeddingModel, Integer tokenCount, String embedStatus) {
        this.id = id;
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.contentHash = contentHash;
        this.embeddingModel = embeddingModel;
        this.tokenCount = tokenCount;
        this.embedStatus = embedStatus;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getEmbedStatus() {
        return embedStatus;
    }

    public void setEmbedStatus(String embedStatus) {
        this.embedStatus = embedStatus;
    }
}
