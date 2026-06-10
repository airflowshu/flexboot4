package com.yunlbd.flexboot4.entity.sys;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_social_account")
public class SysUserSocialAccount extends BaseEntity {

    @Column("user_id")
    private String userId;

    private String provider;

    @Column("provider_user_id")
    private String providerUserId;

    @Column("provider_username")
    private String providerUsername;

    private String nickname;

    @Column("avatar_url")
    private String avatarUrl;

    private String email;

    @Column("email_verified")
    private Boolean emailVerified;

    @Column("bind_time")
    private LocalDateTime bindTime;

    @Column("last_login_time")
    private LocalDateTime lastLoginTime;

    private Integer status;
}
