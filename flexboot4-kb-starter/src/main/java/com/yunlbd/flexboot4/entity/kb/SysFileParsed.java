package com.yunlbd.flexboot4.entity.kb;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.file.parse.ParsedBlock;
import com.yunlbd.flexboot4.mybatis.typehandler.JsonbTypeHandler;
import com.yunlbd.flexboot4.mybatis.typehandler.ParsedBlocksJsonbTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_file_parsed")
@Schema(name = "SysFileParsed")
public class SysFileParsed extends BaseEntity {

    /**
     * 所属文件ID（sys_file.id）
     */
    @Column("file_id")
    private String fileId;

    /**
     * 解析出的全文（便于调试/重切分；可能包含换行与分隔符）
     */
    @Column("full_text")
    private String fullText;

    /**
     * 页数（主要用于 PDF；对于 DOCX/TXT 等可能为 0）
     */
    @Column("page_count")
    private Integer pageCount;

    /**
     * 解析元数据（JSONB），例如 {"type":"docx"}、解析器版本、标题等
     */
    @Column(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> metadata;

    /**
     * 结构化解析块（用于按页/按章节映射 chunk.page_number 与 chunk.section_title）
     */
    @Column(typeHandler = ParsedBlocksJsonbTypeHandler.class)
    private List<ParsedBlock> blocks;
}
