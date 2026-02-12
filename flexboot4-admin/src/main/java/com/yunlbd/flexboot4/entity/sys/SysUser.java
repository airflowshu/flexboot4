package com.yunlbd.flexboot4.entity.sys;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.mybatisflex.annotation.*;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_user")
@Schema(name = "SysUser")
public class SysUser extends BaseEntity {
    @ExcelProperty("登录名")
    private String username;

    @ExcelIgnore //标明不需要导出
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @ExcelProperty("用户名")
    private String realName;

    @ExcelIgnore
    @Schema(description = "头像文件ID，对应 sys_file.id")
    @Column("profile_file_id")
    private String profileFileId;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("手机")
    private String phone;

    @DictEnum("gender")
    @Schema(example = "male", title = "性别值")
    @ExcelIgnore //标明不需要导出
    private String gender;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY) //只在入参中隐藏（响应中可见）
    @ExcelProperty("性别")
    @Column(ignore = true) //非实体表字段需这样声明
    private String genderStr;

    @ExcelProperty("部门ID")
    private String deptId;

    @ExcelProperty("状态")
    private Integer status; // 1: enabled, 0: disabled

    @ExcelIgnore //标明不需要导出
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) //只在入参中隐藏（响应中可见）
    @RelationManyToOne(selfField = "deptId", targetField = "id")
    private SysDept dept;

    @ExcelIgnore //标明不需要导出
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) //只在入参中隐藏（响应中可见）
    @RelationManyToMany(
            joinTable = "sys_user_role",
            selfField = "id", joinSelfColumn = "user_id",
            targetField = "id", joinTargetColumn = "role_id"
    )
    private List<SysRole> roles;

    @RelationOneToOne(selfField = "profileFileId", targetField = "id")
    private SysFile profileFile;
}
