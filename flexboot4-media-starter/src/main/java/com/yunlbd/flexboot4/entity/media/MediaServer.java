package com.yunlbd.flexboot4.entity.media;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("media_server")
@Schema(name = "MediaServer", description = "流媒体服务")
public class MediaServer extends BaseEntity {

    @Schema(description = "服务器名称")
    private String serverName;
    @Schema(description = "服务器类型", example = "ZLMediaKit")
    private String serverType;
    @Schema(description = "API基础地址", example = "http://127.0.0.1:8080")
    private String baseUrl;
    @Schema(description = "API密钥")
    private String apiSecret;
    @Schema(description = "Hook密钥")
    private String hookSecret;
    @Schema(description = "公网主机地址")
    private String publicHost;
    @Schema(description = "播放域名")
    private String playDomain;
    @Schema(description = "RTP服务IP地址")
    private String rtpIp;
    @Schema(description = "RTP端口起始值")
    private Integer rtpPortStart;
    @Schema(description = "RTP端口结束值")
    private Integer rtpPortEnd;
    @Schema(description = "默认流应用名", example = "rtp")
    private String defaultStreamApp;
    @Schema(description = "是否启用Hook回调")
    private Boolean hookEnabled;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "服务器状态", example = "online")
    private String status;
    @Schema(description = "上次测试时间")
    private LocalDateTime lastTestTime;
    @Schema(description = "上次Hook回调时间")
    private LocalDateTime lastHookTime;
    @Schema(description = "最近错误信息")
    private String lastError;
}
