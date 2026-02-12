package com.yunlbd.flexboot4.entity.kb;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_base")
@Schema(name = "SysKnowledgeBase")
public class KnowledgeBase extends BaseEntity {

    @Schema(title = "名称")
    private String name;

    @Schema(title = "描述")
    private String description;

    @Schema(title = "类型: public/team/private")
    private String type;

    @Schema(title = "创建者用户ID")
    private String ownerId;

    @Schema(title = "状态")
    private Integer status;
}
