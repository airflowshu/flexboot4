package com.yunlbd.flexboot4.dto.media;

import lombok.Data;

@Data
public class MediaServerCreateReq {
    private String serverName;
    private String serverType;
    private String baseUrl;
    private String apiSecret;
    private String hookSecret;
    private String publicHost;
    private String playDomain;
    private String rtpIp;
    private Integer rtpPortStart;
    private Integer rtpPortEnd;
    private String defaultStreamApp;
    private Boolean hookEnabled;
    private Boolean enabled;
    private String status;
    private String remark;
}
