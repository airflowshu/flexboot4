package com.yunlbd.flexboot4.aigateway.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 向量存储表 (ai_vector_chunk)
 */
@Table("ai_vector_chunk")
public class AiVectorChunk {

    @Id
    private String id;

    @Column("kb_id")
    private String kbId;

    @Column("chunk_id")
    private String chunkId;

    @Column("file_id")
    private String fileId;

    @Column("embedding_model")
    private String embeddingModel;

    @Column("vector")
    private List<Float> vector;

    @Column("tokens")
    private Integer tokens;

    @Column("created_at")
    private LocalDateTime createdAt;

    public AiVectorChunk() {
    }

    public AiVectorChunk(String kbId, String chunkId, String fileId, String embeddingModel, List<Float> vector, Integer tokens) {
        this.kbId = kbId;
        this.chunkId = chunkId;
        this.fileId = fileId;
        this.embeddingModel = embeddingModel;
        this.vector = vector;
        this.tokens = tokens;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Float> getVector() {
        return vector;
    }

    public void setVector(List<Float> vector) {
        this.vector = vector;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
