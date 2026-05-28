package com.yunlbd.flexboot4.dto.media;

import lombok.Data;

@Data
public class MediaChannelCreateReq {
    private String serverId;
    private String gatewayId;
    private String deviceId;
    private String parentChannelId;
    private String channelName;
    private String channelCode;
    private String channelType;
    private String manufacturer;
    private String model;
    private String owner;
    private String civilCode;
    private String address;
    private String ptzType;
    private Boolean hasRecord;
    private String status;
    private String playStatus;
    private String longitude;
    private String latitude;
    private String fixedUrl;
    private String streamApp;
    private String streamId;
    private String remark;
}
