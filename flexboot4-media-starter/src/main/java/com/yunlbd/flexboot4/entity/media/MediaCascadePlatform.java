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
@Table("media_cascade_platform")
@Schema(name = "MediaCascadePlatform", description = "级联平台")
public class MediaCascadePlatform extends BaseEntity {

    @Schema(description = "所属服务器ID")
    private String serverId;
    @Schema(description = "所属网关ID")
    private String gatewayId;
    @Schema(description = "平台名称", example = "上级平台")
    private String platformName;
    @Schema(description = "平台编码", example = "34020000002000000001")
    private String platformCode;
    @Schema(description = "SIP国标ID", example = "34020000002000000001")
    private String sipId;
    @Schema(description = "SIP域")
    private String sipDomain;
    @Schema(description = "SIP密码")
    private String sipPassword;
    @Schema(description = "平台主机地址", example = "192.168.1.100")
    private String host;
    @Schema(description = "平台端口", example = "5060")
    private Integer port;
    @Schema(description = "传输协议", example = "UDP/TCP")
    private String transport;
    @Schema(description = "厂商")
    private String manufacturer;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "在线状态", example = "ONLINE/OFFLINE")
    private String onlineStatus;
    @Schema(description = "心跳间隔(秒)")
    private Integer heartbeatIntervalSeconds;
    @Schema(description = "注册过期时间(秒)")
    private Integer registerExpiresSeconds;
    @Schema(description = "上次注册时间")
    private LocalDateTime lastRegisterTime;
    @Schema(description = "上次心跳时间")
    private LocalDateTime lastKeepaliveTime;
    @Schema(description = "最近错误信息")
    private String lastError;

    @RelationManyToOne(selfField = "gatewayId", targetField = "id")
    @Column(ignore = true)
    private MediaGateway gateway;

    @RelationOneToMany(selfField = "id", targetField = "platformId")
    @Column(ignore = true)
    private java.util.List<MediaCascadeBinding> bindings;
}
