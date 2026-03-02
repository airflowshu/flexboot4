package com.yunlbd.flexboot4.entity.kb;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 知识库成员表实体（团队类型使用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("kb_member")
@Schema(name = "SysKbMember")
public class KbMember extends BaseEntity {

    @Schema(title = "知识库ID")
    private String kbId;

    @Schema(title = "用户ID")
    private String userId;
}
