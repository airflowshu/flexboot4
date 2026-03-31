package com.yunlbd.flexboot4.entity.media;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("media_screen")
@Schema(name = "MediaScreen", description = "分屏方案")
public class MediaScreen extends BaseEntity {

    @Schema(description = "分屏方案名称", example = "4分屏")
    private String screenName;
    @Schema(description = "布局类型", example = "grid/custom")
    private String layoutType;
    @Schema(description = "布局JSON配置")
    private String layoutJson;
    @Schema(description = "是否启用")
    private Boolean enabled;
    @Schema(description = "是否默认方案")
    private Boolean isDefault;

    @RelationOneToMany(selfField = "id", targetField = "screenId")
    @Column(ignore = true)
    private List<MediaScreenSlot> slots;
}
