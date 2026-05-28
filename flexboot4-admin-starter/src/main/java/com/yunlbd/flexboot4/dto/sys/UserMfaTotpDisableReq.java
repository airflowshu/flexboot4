package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserMfaTotpDisableReq {

    @NotBlank
    private String password;

    @NotBlank
    private String code;
}
