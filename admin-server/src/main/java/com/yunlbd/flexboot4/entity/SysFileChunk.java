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
@Table("sys_file_chunk")
@Schema(name = "SysFileChunk")
public class SysFileChunk extends BaseEntity {

    private String fileId;

    private Integer chunkIndex;

    private String content;

    private String contentHash;

    private String embeddingId;

    private String embeddingModel;

    private Integer tokenCount;

    private String embedStatus;

    private Integer pageNumber;

    private String sectionTitle;
}
