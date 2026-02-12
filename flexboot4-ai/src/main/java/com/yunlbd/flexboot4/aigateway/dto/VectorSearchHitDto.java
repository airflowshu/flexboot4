package com.yunlbd.flexboot4.aigateway.dto;

public class VectorSearchHitDto {

    private String chunkId;
    private String fileId;
    private Integer tokens;
    private Double distance;

    public VectorSearchHitDto() {
    }

    public VectorSearchHitDto(String chunkId, String fileId, Integer tokens, Double distance) {
        this.chunkId = chunkId;
        this.fileId = fileId;
        this.tokens = tokens;
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

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
