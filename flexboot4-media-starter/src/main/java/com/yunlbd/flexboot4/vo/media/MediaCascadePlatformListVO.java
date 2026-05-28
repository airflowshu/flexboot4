package com.yunlbd.flexboot4.vo.media;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaCascadePlatformListVO extends BaseAuditVO {
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRegisterTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastKeepaliveTime;
    private String lastError;
    private MediaGatewayListVO gateway;
}
