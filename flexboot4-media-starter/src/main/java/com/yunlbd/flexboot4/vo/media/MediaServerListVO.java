package com.yunlbd.flexboot4.vo.media;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class MediaServerListVO extends BaseAuditVO {
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTestTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHookTime;
    private String lastError;
}
