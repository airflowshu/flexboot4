package com.yunlbd.flexboot4.entity.ops;

import com.alibaba.excel.annotation.ExcelProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 系统参数配置表
 * 用于存储系统的配置参数，通过key:value方式管理
 *
 * @author Wangts
 * @since 2026年01月29日
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_config")
@Schema(name = "SysConfig")
public class SysConfig extends BaseEntity {

    @ExcelProperty("配置键")
    @Schema(description = "配置键", example = "system.name")
    private String configKey;

    @ExcelProperty("配置值")
    @Schema(description = "配置值", example = "FlexBoot4")
    private String configValue;

    @ExcelProperty("配置类型")
    @Schema(description = "配置类型: STRING/NUMBER/BOOLEAN/JSON/ARRAY", example = "STRING")
    private String configType;

    @ExcelProperty("配置描述")
    @Schema(description = "配置描述")
    private String description;

    @Schema(description = "状态: 1-启用, 0-禁用")
    @DictEnum("status")
    private Integer status;

    @ExcelProperty("状态")
    @Column(ignore = true)
    @Schema(description = "状态: 1-启用, 0-禁用")
    private String statusStr;
}