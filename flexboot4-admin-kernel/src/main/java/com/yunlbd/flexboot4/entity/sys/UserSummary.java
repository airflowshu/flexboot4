package com.yunlbd.flexboot4.entity.sys;

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
@Table("sys_user")
@Schema(name = "UserSummary")
public class UserSummary extends BaseEntity {

    @Schema(title = "登录名")
    private String username;

    @Schema(title = "用户名")
    private String realName;

    @Schema(title = "头像文件ID")
    private String profileFileId;
}
