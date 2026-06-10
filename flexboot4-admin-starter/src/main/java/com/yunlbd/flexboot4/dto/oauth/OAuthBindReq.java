package com.yunlbd.flexboot4.dto.oauth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OAuthBindReq {

    @NotBlank(message = "绑定票据不能为空")
    private String bindTicket;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
