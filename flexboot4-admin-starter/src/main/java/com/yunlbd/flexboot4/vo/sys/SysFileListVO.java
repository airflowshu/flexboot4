package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysFileListVO extends BaseAuditVO {
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
}
