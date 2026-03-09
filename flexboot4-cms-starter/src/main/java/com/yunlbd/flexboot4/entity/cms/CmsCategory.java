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
 * CMS 栏目实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("cms_category")
@Schema(name = "CmsCategory", description = "CMS栏目")
public class CmsCategory extends BaseEntity {

    @Schema(title = "父栏目ID")
    private String parentId;

    @Schema(title = "栏目名称")
    private String categoryName;

    @Schema(title = "栏目编码")
    private String categoryCode;

    @Schema(title = "栏目描述")
    private String description;

    @Schema(title = "封面文件ID")
    private String coverFileId;

    @Schema(title = "排序号")
    private Integer sortOrder;

    @Schema(title = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(title = "封面文件对象", accessMode = Schema.AccessMode.READ_ONLY)
    @RelationOneToOne(selfField = "coverFileId", targetField = "id")
    @Column(ignore = true)
    private SysFile coverFile;
}

