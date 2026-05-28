package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CurrentUserPasswordUpdateReq {

    @NotBlank(message = "请输入旧密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 8, max = 100, message = "新密码长度需为 8 到 100 位")
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}
