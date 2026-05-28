package com.yunlbd.flexboot4.dto.sys;

import lombok.Data;

@Data
public class SysFileCreateReq {
    private String tenantId;
    private String bizType;
    private String bizId;
    private String fileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String fileHash;
    private String storageType;
    private String bucketName;
    private String objectKey;
    private String aiStatus;
    private String aiParseStatus;
    private String aiEmbedStatus;
    private Integer chunkCount;
    private Integer tokenEstimate;
    private String embeddingModel;
    private String remark;
}
