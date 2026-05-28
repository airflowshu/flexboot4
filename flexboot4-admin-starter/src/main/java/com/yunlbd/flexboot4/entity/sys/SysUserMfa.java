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
@Table("sys_user_mfa")
public class SysUserMfa extends BaseEntity {

    @Column("user_id")
    private String userId;

    private String type;

    @Column("secret_ciphertext")
    private String secretCiphertext;

    @Column("device_name")
    private String deviceName;

    private Boolean enabled;

    @Column("confirmed_at")
    private LocalDateTime confirmedAt;

    @Column("last_used_at")
    private LocalDateTime lastUsedAt;
}
