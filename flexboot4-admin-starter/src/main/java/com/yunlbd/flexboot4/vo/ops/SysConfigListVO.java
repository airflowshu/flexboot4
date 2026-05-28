package com.yunlbd.flexboot4.vo.ops;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigListVO extends BaseAuditVO {
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private Integer status;
    private String statusStr;
}
