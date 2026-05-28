package com.yunlbd.flexboot4.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SmsCodeReq {

    @NotBlank
    private String phone;
}
