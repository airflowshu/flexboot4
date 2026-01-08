package com.yunlbd.flexboot4.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.mybatisflex.annotation.Table;
import lombok.*;

/**
 * 部门表 实体类。
 *
 * @author yunlbd_wts
 * @since 2026-01-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_dept")
public class SysDept extends BaseEntity {

    /**
     * 部门名称
     */
    @ExcelProperty("部门名称")
    private String deptName;

    /**
     * 排序
     */
    @ExcelProperty("排序")
    private Integer orderNo;

    /**
     * 状态 1-启用 0-禁用
     */
    @ExcelProperty("状态")
    private Integer status;

    /**
     * 父级ID
     */
    @ExcelIgnore
    @ExcelProperty("父级ID")
    private String parentId;

}
