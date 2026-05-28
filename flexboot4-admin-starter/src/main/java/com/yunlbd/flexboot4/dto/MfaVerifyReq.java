package com.yunlbd.flexboot4.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyReq {

    @NotBlank
    private String challengeToken;

    @NotBlank
    private String code;
}
