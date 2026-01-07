package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String avatar;
    private String email;
    private String phone;
    private Integer status; // 1: enabled, 0: disabled

    @RelationManyToMany(
            joinTable = "sys_user_role",
            selfField = "id", joinSelfColumn = "user_id",
            targetField = "id", joinTargetColumn = "role_id"
    )
    private List<SysRole> roles;
}
