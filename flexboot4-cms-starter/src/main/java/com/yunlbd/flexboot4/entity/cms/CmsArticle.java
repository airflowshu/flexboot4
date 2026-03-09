package com.yunlbd.flexboot4.entity.cms;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CMS 文章实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("cms_article")
@Schema(name = "CmsArticle", description = "CMS文章")
public class CmsArticle extends BaseEntity {

    @Schema(title = "文章标题")
    private String title;

    @Schema(title = "栏目ID")
    private String categoryId;

    @Schema(title = "作者")
    private String author;

    @Schema(title = "封面文件ID")
    private String coverFileId;

    @Schema(title = "文章摘要")
    private String summary;

    @Schema(title = "文章内容")
    private String content;

    @Schema(title = "浏览次数")
    private Integer viewCount;

    @Schema(title = "点赞次数")
    private Integer likeCount;

    @Schema(title = "文章状态：DRAFT/PENDING/PUBLISHED/REJECTED")
    @DictEnum("cms_article_status")
    private String status;

    @Schema(title = "文章状态：草稿/待审核/已发布/已驳回", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(ignore = true)
    private String statusStr;

    @Schema(title = "发布时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    @Schema(title = "审核人ID")
    private String reviewerId;

    @Schema(title = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    @Schema(title = "审核意见")
    private String reviewComment;

    @Schema(title = "排序号")
    private Integer sortOrder;

    @Schema(title = "所属栏目", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "categoryId", targetField = "id")
    @Column(ignore = true)
    private CmsCategory category;

    @Schema(title = "封面文件", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "coverFileId", targetField = "id")
    @Column(ignore = true)
    private SysFile coverFile;

    @Schema(title = "审核人", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "reviewerId", targetField = "id")
    @Column(ignore = true)
    private SysUser reviewer;

    @Schema(title = "标签列表", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationManyToMany(
            joinTable = "cms_article_tag",
            selfField = "id", joinSelfColumn = "article_id",
            targetField = "id", joinTargetColumn = "tag_id"
    )
    @Column(ignore = true)
    private List<CmsTag> tags;
}

