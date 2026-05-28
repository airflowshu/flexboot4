package com.yunlbd.flexboot4.dto.sms;

import lombok.Data;

import java.util.Map;

@Data
public class Sms4jConfigCreateReq {
    private String configName;
    private String supplierType;
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
    private String remark;
}
