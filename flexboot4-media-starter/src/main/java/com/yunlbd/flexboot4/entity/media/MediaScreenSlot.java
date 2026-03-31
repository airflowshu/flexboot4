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
@Table("media_screen_slot")
@Schema(name = "MediaScreenSlot", description = "分屏槽位")
public class MediaScreenSlot extends BaseEntity {

    @Schema(description = "所属分屏方案ID")
    private String screenId;
    @Schema(description = "槽位索引", example = "0")
    private Integer slotIndex;
    @Schema(description = "槽位名称", example = "左上角")
    private String slotName;
    @Schema(description = "X坐标")
    private Integer x;
    @Schema(description = "Y坐标")
    private Integer y;
    @Schema(description = "宽度")
    private Integer width;
    @Schema(description = "高度")
    private Integer height;
    @Schema(description = "绑定的通道ID")
    private String channelId;
    @Schema(description = "会话类型", example = "LIVE/PLAYBACK")
    private String sessionType;
    @Schema(description = "其他配置JSON")
    private String optionsJson;

    @RelationManyToOne(selfField = "channelId", targetField = "id")
    @Column(ignore = true)
    private MediaChannel channel;
}
