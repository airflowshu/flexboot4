package com.yunlbd.flexboot4.entity.cms;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * CMS 标签实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("cms_tag")
@Schema(name = "CmsTag", description = "CMS标签")
public class CmsTag extends BaseEntity {

    @Schema(title = "标签名称")
    private String tagName;

    @Schema(title = "标签颜色")
    private String tagColor;

    @Schema(title = "使用次数")
    private Integer useCount;
}

