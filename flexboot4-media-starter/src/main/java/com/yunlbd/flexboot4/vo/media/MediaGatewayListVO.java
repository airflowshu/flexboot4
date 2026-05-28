package com.yunlbd.flexboot4.vo.media;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaGatewayListVO extends BaseAuditVO {
    private String serverId;
    private String gatewayName;
    private String gatewayCode;
    private String sipId;
    private String sipDomain;
    private String sipPassword;
    private String localIp;
    private Integer localPort;
    private String publicIp;
    private Integer publicPort;
    private String transport;
    private String rtpIp;
    private Integer rtpPortStart;
    private Integer rtpPortEnd;
    private Integer heartbeatIntervalSeconds;
    private Integer registerExpiresSeconds;
    private Integer catalogSubscribeCycleSeconds;
    private Integer threadPoolSize;
    private Boolean enabled;
    private Boolean active;
    private String runtimeStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastStopTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastKeepaliveTime;
    private String lastError;
    private MediaServerListVO server;
}
