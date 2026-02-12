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
@Table("sys_user_role")
@Schema(name = "SysUserRole")
public class SysUserRole extends BaseEntity {
    private Long userId;
    private Long roleId;

    // 虽然 IDEA 提示该参数是“冗余”的，但强烈建议不要移除它。
    // 在当前这种“子类覆盖父类配置”的特殊场景下，显式写出 isLogicDelete = false 的意义远大于它的技术冗余。
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
