package com.yunlbd.flexboot4.entity.kb;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
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

    /**
     * 所属文件ID（sys_file.id）
     */
    private String fileId;

    /**
     * 分片序号（从 0 开始）
     */
    private Integer chunkIndex;

    /**
     * 分片文本内容（用于向量化）
     */
    private String content;

    /**
     * 分片内容哈希（通常为 content 的 SHA-256，用于去重/幂等）
     */
    private String contentHash;

    /**
     * 向量化后的ID（由向量服务写回，例如 Milvus/PGVector/ES 等的主键）
     */
    private String embeddingId;

    /**
     * 使用的向量模型（例如 text-embedding-3-large）；可与 sys_file.embedding_model 对应
     */
    private String embeddingModel;

    /**
     * 本分片 token 估算值（用于费用/长度控制；为近似估算）
     */
    private Integer tokenCount;

    /**
     * 向量化状态（PENDING/RUNNING/SUCCESS/FAILED 等）
     */
    private String embedStatus;

    /**
     * 页码（通常用于 PDF 等按页解析的文档；未实现时可能为 0 或 null）
     */
    private Integer pageNumber;

    /**
     * 章节标题/小节标题（用于结构化检索；未实现时可能为 null）
     */
    private String sectionTitle;
}
