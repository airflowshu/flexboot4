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

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("media_cascade_binding")
@Schema(name = "MediaCascadeBinding", description = "级联绑定")
public class MediaCascadeBinding extends BaseEntity {

    @Schema(description = "所属级联平台ID")
    private String platformId;
    @Schema(description = "本地通道ID")
    private String channelId;
    @Schema(description = "级联平台通道编码(国标ID)", example = "34020000001320000001")
    private String gbChannelCode;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "是否启用直播")
    private Boolean liveEnabled;
    @Schema(description = "是否启用回放")
    private Boolean playbackEnabled;

    @RelationManyToOne(selfField = "platformId", targetField = "id")
    @Column(ignore = true)
    private MediaCascadePlatform platform;

    @RelationManyToOne(selfField = "channelId", targetField = "id")
    @Column(ignore = true)
    private MediaChannel channel;
}
