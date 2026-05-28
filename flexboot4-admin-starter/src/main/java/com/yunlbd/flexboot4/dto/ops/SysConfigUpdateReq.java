package com.yunlbd.flexboot4.dto.ops;

import lombok.Data;

@Data
public class SysConfigUpdateReq {
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Integer status;
    private String remark;
}
