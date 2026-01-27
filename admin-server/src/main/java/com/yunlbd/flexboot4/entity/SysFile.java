package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_file")
@Schema(name = "SysFile")
public class SysFile extends BaseEntity {

    private String tenantId;

    private String projectId;

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
