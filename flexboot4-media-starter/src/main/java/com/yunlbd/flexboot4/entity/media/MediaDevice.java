package com.yunlbd.flexboot4.entity.media;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("media_device")
@Schema(name = "MediaDevice", description = "视频设备")
public class MediaDevice extends BaseEntity {

    @Schema(description = "所属服务器ID")
    private String serverId;
    @Schema(description = "所属网关ID")
    private String gatewayId;
    @Schema(description = "设备名称", example = "摄像头01")
    private String deviceName;
    @Schema(description = "设备编码(国标ID)", example = "34020000001320000001")
    private String deviceCode;
    @Schema(description = "接入类型", example = "GB28181/FIXED_ADDRESS")
    private String accessType;
    @Schema(description = "厂商")
    private String manufacturer;
    @Schema(description = "型号")
    private String model;
    @Schema(description = "设备归属者")
    private String owner;
    @Schema(description = "行政区域编码", example = "340200")
    private String civilCode;
    @Schema(description = "设备地址")
    private String address;
    @Schema(description = "IP地址")
    private String ip;
    @Schema(description = "端口号", example = "5060")
    private Integer port;
    @Schema(description = "用户名")
    private String username;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "媒体流地址")
    private String mediaUrl;
    @Schema(description = "流模式", example = "UDP/TCP")
    private String streamMode;
    @Schema(description = "在线状态", example = "ONLINE/OFFLINE")
    private String onlineStatus;
    @Schema(description = "注册状态", example = "REGISTERED/UNREGISTERED")
    private String registerStatus;
    @Schema(description = "上次注册时间")
    private LocalDateTime lastRegisterTime;
    @Schema(description = "上次心跳时间")
    private LocalDateTime lastKeepaliveTime;
    @Schema(description = "上次Catalog时间")
    private LocalDateTime lastCatalogTime;

    @RelationManyToOne(selfField = "serverId", targetField = "id")
    @Column(ignore = true)
    private MediaServer server;

    @RelationManyToOne(selfField = "gatewayId", targetField = "id")
    @Column(ignore = true)
    private MediaGateway gateway;

    @RelationOneToMany(selfField = "id", targetField = "deviceId")
    @Column(ignore = true)
    private List<MediaChannel> channels;
}
