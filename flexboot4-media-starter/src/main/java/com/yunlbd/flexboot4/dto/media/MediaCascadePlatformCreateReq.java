package com.yunlbd.flexboot4.dto.media;

import lombok.Data;

@Data
public class MediaCascadePlatformCreateReq {
    private String serverId;
    private String gatewayId;
    private String platformName;
    private String platformCode;
    private String sipId;
    private String sipDomain;
    private String sipPassword;
    private String host;
    private Integer port;
    private String transport;
    private String manufacturer;
    private Boolean enabled;
    private String onlineStatus;
    private Integer heartbeatIntervalSeconds;
    private Integer registerExpiresSeconds;
    private String lastError;
    private String remark;
}
