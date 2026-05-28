package com.yunlbd.flexboot4.vo.media;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaChannelListVO extends BaseAuditVO {
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPlayTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOfflineTime;
}
