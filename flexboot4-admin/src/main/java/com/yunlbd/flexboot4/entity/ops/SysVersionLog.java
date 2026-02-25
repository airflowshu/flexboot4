package com.yunlbd.flexboot4.entity.ops;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.mybatis.typehandler.StringListJsonbTypeHandler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_version_log")
@Schema(name = "SysVersionLog")
public class SysVersionLog extends BaseEntity {

    @Schema(title = "版本号")
    private String versionNo;

    @Schema(title = "发布日期")
    private LocalDateTime releaseDate;

    @DictEnum("version_type")
    @Schema(title = "类型")
    private String type;

    @Schema(title = "类型Str")
    @Column(ignore = true)
    private String typeStr;

    @Schema(title = "标题")
    private String title;

    @Schema(title = "更新描述")
    private String description;

    @Schema(title = "状态:0-草稿,1-已发布")
    private Integer status;

    // 声明处理器，自动完成 JSON 字符串与 List 的转换
    @Column(typeHandler = StringListJsonbTypeHandler.class)
    private List<String> features;

    // 声明处理器，自动完成 JSON 字符串与 List 的转换
    @Column(typeHandler = StringListJsonbTypeHandler.class)
    private List<String> fixes;
}
