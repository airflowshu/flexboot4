package com.yunlbd.flexboot4.dto.ops;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysConfigCreateReq {
    @NotBlank
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Integer status;
    private String remark;
}
