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
@Table("media_stream_session")
@Schema(name = "MediaStreamSession", description = "播放会话")
public class MediaStreamSession extends BaseEntity {

    @Schema(description = "所属服务器ID")
    private String serverId;
    @Schema(description = "所属网关ID")
    private String gatewayId;
    @Schema(description = "设备ID")
    private String deviceId;
    @Schema(description = "通道ID")
    private String channelId;
    @Schema(description = "会话类型", example = "LIVE/PLAYBACK")
    private String sessionType;
    @Schema(description = "流应用名", example = "rtp/proxy")
    private String streamApp;
    @Schema(description = "流ID")
    private String streamId;
    @Schema(description = "播放协议", example = "FLV/WS-FLV/HLS/RTSP")
    private String playProtocol;
    @Schema(description = "播放地址")
    private String playUrl;
    @Schema(description = "代理密钥")
    private String proxyKey;
    @Schema(description = "SSRC标识")
    private String ssrc;
    @Schema(description = "SIP Dialog ID")
    private String dialogId;
    @Schema(description = "RTP端口")
    private Integer rtpPort;
    @Schema(description = "当前观看人数")
    private Integer viewerCount;
    @Schema(description = "会话开始时间")
    private LocalDateTime startedTime;
    @Schema(description = "会话结束时间")
    private LocalDateTime endedTime;
    @Schema(description = "会话状态", example = "STREAMING/CLOSED")
    private String status;

    @RelationManyToOne(selfField = "channelId", targetField = "id")
    @Column(ignore = true)
    private MediaChannel channel;
}
