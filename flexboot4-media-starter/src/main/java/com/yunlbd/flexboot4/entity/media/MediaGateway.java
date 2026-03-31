package com.yunlbd.flexboot4.entity.media;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationManyToOne;
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
@Table("media_gateway")
@Schema(name = "MediaGateway", description = "视频网关")
public class MediaGateway extends BaseEntity {

    @Schema(description = "所属服务器ID")
    private String serverId;
    @Schema(description = "网关名称", example = "主网关")
    private String gatewayName;
    @Schema(description = "网关编码", example = "34020000001110000001")
    private String gatewayCode;
    @Schema(description = "SIP国标ID", example = "34020000001110000001")
    private String sipId;
    @Schema(description = "SIP域")
    private String sipDomain;
    @Schema(description = "SIP密码")
    private String sipPassword;
    @Schema(description = "本地IP地址")
    private String localIp;
    @Schema(description = "本地端口", example = "5060")
    private Integer localPort;
    @Schema(description = "公网IP地址")
    private String publicIp;
    @Schema(description = "公网端口", example = "15060")
    private Integer publicPort;
    @Schema(description = "传输协议", example = "UDP/TCP")
    private String transport;
    @Schema(description = "RTP服务IP")
    private String rtpIp;
    @Schema(description = "RTP端口起始值")
    private Integer rtpPortStart;
    @Schema(description = "RTP端口结束值")
    private Integer rtpPortEnd;
    @Schema(description = "心跳间隔(秒)", example = "60")
    private Integer heartbeatIntervalSeconds;
    @Schema(description = "注册过期时间(秒)", example = "3600")
    private Integer registerExpiresSeconds;
    @Schema(description = "目录订阅周期(秒)", example = "300")
    private Integer catalogSubscribeCycleSeconds;
    @Schema(description = "线程池大小", example = "4")
    private Integer threadPoolSize;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "是否激活")
    private Boolean active;
    @Schema(description = "运行时状态", example = "running/stopped")
    private String runtimeStatus;
    @Schema(description = "上次启动时间")
    private LocalDateTime lastStartTime;
    @Schema(description = "上次停止时间")
    private LocalDateTime lastStopTime;
    @Schema(description = "上次心跳时间")
    private LocalDateTime lastKeepaliveTime;
    @Schema(description = "最近错误信息")
    private String lastError;

    @RelationManyToOne(selfField = "serverId", targetField = "id")
    @Column(ignore = true)
    private MediaServer server;
}
