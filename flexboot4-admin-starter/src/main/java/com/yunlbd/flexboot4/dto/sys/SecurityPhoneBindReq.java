package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SecurityPhoneBindReq {

    @NotBlank
    private String phone;

    @NotBlank
    private String code;
}
