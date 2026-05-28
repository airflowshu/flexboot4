package com.yunlbd.flexboot4.dto.media;

import lombok.Data;

@Data
public class MediaDeviceCreateReq {
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
    private String remark;
}
