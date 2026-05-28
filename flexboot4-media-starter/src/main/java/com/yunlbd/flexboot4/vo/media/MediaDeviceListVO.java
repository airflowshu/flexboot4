package com.yunlbd.flexboot4.vo.media;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaDeviceListVO extends BaseAuditVO {
    private String serverId;
    private String gatewayId;
    private String deviceName;
    private String deviceCode;
    private String accessType;
    private String manufacturer;
    private String model;
    private String owner;
    private String civilCode;
    private String address;
    private String ip;
    private Integer port;
    private String username;
    private String password;
    private String mediaUrl;
    private String streamMode;
    private String onlineStatus;
    private String registerStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRegisterTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastKeepaliveTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCatalogTime;
    private MediaServerListVO server;
    private MediaGatewayListVO gateway;
    private List<MediaChannelListVO> channels;
}
