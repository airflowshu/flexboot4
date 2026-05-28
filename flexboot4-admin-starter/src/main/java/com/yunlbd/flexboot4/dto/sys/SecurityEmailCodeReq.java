package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SecurityEmailCodeReq {

    @NotBlank
    @Email
    private String email;
}
