package com.yunlbd.flexboot4.entity.cms;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * CMS 文章标签关联实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("cms_article_tag")
@Schema(name = "CmsArticleTag", description = "CMS文章标签关联")
public class CmsArticleTag extends BaseEntity {

    @Schema(title = "文章ID")
    private String articleId;

    @Schema(title = "标签ID")
    private String tagId;

    @Schema(title = "关联文章", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "articleId", targetField = "id")
    @Column(ignore = true)
    private CmsArticle article;

    @Schema(title = "标签对象", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "tagId", targetField = "id")
    @Column(ignore = true)
    private CmsTag tag;
}

