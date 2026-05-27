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
 * CMS 模板发布记录。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("cms_template_publish_record")
@Schema(name = "CmsTemplatePublishRecord", description = "CMS 模板发布记录")
public class CmsTemplatePublishRecord extends BaseEntity {

    @Schema(title = "发布名称")
    private String publishName;

    @Schema(title = "模板根目录")
    private String templateRootDir;

    @Schema(title = "发布目录")
    private String publishDir;

    @Schema(title = "压缩包物理路径")
    private String zipFilePath;

    @Schema(title = "发布首页相对URL")
    private String indexRelativeUrl;

    @Schema(title = "压缩包相对URL")
    private String zipRelativeUrl;

    @Schema(title = "发布文件数")
    private Integer fileCount;

    @Schema(title = "发布状态：SUCCESS/FAILED")
    private String status;

    @Schema(title = "错误信息")
    private String errorMessage;
}
