package com.yunlbd.flexboot4.entity.sys;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_menu")
@Schema(name = "SysRoleMenu")
public class SysRoleMenu extends BaseEntity {
    private String roleId;
    private String menuId;
    /**
     * 覆盖父类的 delFlag 属性
     * 显式指定 isLogicDelete = false，此时对该实体的 delete 操作将变为物理删除
     */
    @Column(isLogicDelete = false)
    @JsonIgnore
    @ExcelIgnore
    @Schema(hidden = true)
    private Integer delFlag;
}
