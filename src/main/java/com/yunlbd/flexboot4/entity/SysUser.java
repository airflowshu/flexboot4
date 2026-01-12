package com.yunlbd.flexboot4.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.excel.ExcelDict;
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
public class SysUser extends BaseEntity {
    @ExcelProperty("登录名")
    private String username;
    @JsonIgnore
    @ExcelIgnore //标明不需要导出
    private String password;
    @ExcelProperty("用户名")
    private String realName;
    @ExcelProperty("头像")
    private String avatar;
    @ExcelProperty("邮箱")
    private String email;
    @ExcelProperty("手机")
    private String phone;
    @ExcelDict("gender")
    @ExcelIgnore //标明不需要导出
    private String gender;
    @ExcelProperty("性别")
    @Column(ignore = true)
    private String genderStr;
    @ExcelProperty("部门ID")
    private String deptId;
    @ExcelProperty("状态")
    private Integer status; // 1: enabled, 0: disabled

    @RelationManyToOne(selfField = "deptId", targetField = "id")
    private SysDept dept;

    @RelationManyToMany(
            joinTable = "sys_user_role",
            selfField = "id", joinSelfColumn = "user_id",
            targetField = "id", joinTargetColumn = "role_id"
    )
    private List<SysRole> roles;
}
