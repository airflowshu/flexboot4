package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserMfaTotpConfirmReq {

    @NotBlank
    private String code;

    private String deviceName;
}
