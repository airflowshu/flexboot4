package com.yunlbd.flexboot4.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 部门表 实体类。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_dept")
@Schema(name = "SysDept")
public class SysDept extends BaseEntity {

    /**
     * 部门名称
     */
    @ExcelProperty("部门名称")
    @Schema(example = "研发部", title = "部门名称")
    private String deptName;

    /**
     * 排序
     */
    @ExcelProperty("排序")
    @Schema(example = "10", title = "排序")
    private Integer orderNo;

    /**
     * 状态 1-启用 0-禁用
     */

    @ExcelIgnore //标明不需要导出
    @DictEnum("dept_status")
    @Schema(example = "1", title = "状态")
    private Integer status;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "状态") //只在入参中隐藏（响应中可见）
    @ExcelProperty("状态")
    @Column(ignore = true)
    private String statusStr;

    /**
     * 父级ID
     */
    @ExcelIgnore
    @ExcelProperty("父级ID")
    @Schema(example = "0", title = "父级ID")
    private String parentId;

}
