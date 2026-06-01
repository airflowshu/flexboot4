package com.yunlbd.flexboot4.vo.sms;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class Sms4jConfigListVO extends BaseAuditVO {
    private String configName;
    private String supplierType;
    private String supplierTypeStr;
    private String configId;
    private String accessKeyId;
    private String accessKeySecret;
    private String signature;
    private String templateId;
    private String sdkAppId;
    private Integer weight;
    private Integer isDefault;
    private Map<String, Object> extParams;
    private Integer status;
    private String statusStr;
    private String testStatus;
    private LocalDateTime lastTestTime;
    private String lastTestMessage;
}
