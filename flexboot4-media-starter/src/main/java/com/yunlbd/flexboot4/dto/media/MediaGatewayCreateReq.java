package com.yunlbd.flexboot4.dto.media;

import lombok.Data;

@Data
public class MediaGatewayCreateReq {
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
    private String remark;
}
