package com.yunlbd.flexboot4.entity.cms;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * CMS 文章附件关联实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("cms_article_file")
@Schema(name = "CmsArticleFile", description = "CMS文章附件")
public class CmsArticleFile extends BaseEntity {

    @Schema(title = "文章ID")
    private String articleId;

    @Schema(title = "文件ID")
    private String fileId;

    @Schema(title = "排序号")
    private Integer sortOrder;

    @Schema(title = "关联文章", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "articleId", targetField = "id")
    @Column(ignore = true)
    private CmsArticle article;

    @Schema(title = "文件对象", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "fileId", targetField = "id")
    @Column(ignore = true)
    private SysFile file;
}

