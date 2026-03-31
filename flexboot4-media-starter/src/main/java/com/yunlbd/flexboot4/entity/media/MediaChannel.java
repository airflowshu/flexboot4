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
@Table("media_channel")
@Schema(name = "MediaChannel", description = "视频通道")
public class MediaChannel extends BaseEntity {

    @Schema(description = "所属服务器ID")
    private String serverId;
    @Schema(description = "所属网关ID")
    private String gatewayId;
    @Schema(description = "所属设备ID")
    private String deviceId;
    @Schema(description = "父通道ID(通道分组)")
    private String parentChannelId;
    @Schema(description = "通道名称", example = "01通道")
    private String channelName;
    @Schema(description = "通道编码(国标ID)", example = "34020000001320000001")
    private String channelCode;
    @Schema(description = "通道类型", example = "GB28281")
    private String channelType;
    @Schema(description = "厂商")
    private String manufacturer;
    @Schema(description = "型号")
    private String model;
    @Schema(description = "归属者")
    private String owner;
    @Schema(description = "行政区域编码")
    private String civilCode;
    @Schema(description = "安装地址")
    private String address;
    @Schema(description = "云台类型", example = "PTZ/FAST/NO")
    private String ptzType;
    @Schema(description = "是否支持录像")
    private Boolean hasRecord;
    @Schema(description = "设备状态", example = "ON/OFF")
    private String status;
    @Schema(description = "播放状态", example = "ONLINE/STOPPED")
    private String playStatus;
    @Schema(description = "经度")
    private String longitude;
    @Schema(description = "纬度")
    private String latitude;
    @Schema(description = "固定推流地址")
    private String fixedUrl;
    @Schema(description = "流应用名")
    private String streamApp;
    @Schema(description = "流ID")
    private String streamId;
    @Schema(description = "上次播放时间")
    private LocalDateTime lastPlayTime;
    @Schema(description = "上次离线时间")
    private LocalDateTime lastOfflineTime;

    @RelationManyToOne(selfField = "deviceId", targetField = "id")
    @Column(ignore = true)
    private MediaDevice device;
}
