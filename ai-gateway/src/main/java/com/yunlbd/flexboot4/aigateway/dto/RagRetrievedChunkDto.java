package com.yunlbd.flexboot4.aigateway.dto;

public class RagRetrievedChunkDto {

    private String chunkId;
    private String fileId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private Double distance;

    public RagRetrievedChunkDto() {
    }

    public RagRetrievedChunkDto(String chunkId, String fileId, Integer chunkIndex, String content, Integer tokenCount, Double distance) {
        this.chunkId = chunkId;
        this.fileId = fileId;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.tokenCount = tokenCount;
        this.distance = distance;
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

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
