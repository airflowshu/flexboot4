package com.yunlbd.flexboot4.dto;

import lombok.Data;

@Data
public class LoginReq {
    private String loginType;
    private String username;
    private String password;
    private String phone;
    private String code;
}
