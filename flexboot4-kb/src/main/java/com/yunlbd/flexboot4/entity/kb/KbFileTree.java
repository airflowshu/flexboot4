package com.yunlbd.flexboot4.entity.kb;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.entity.sys.SysFile;
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
@Table("kb_file_tree")
@Schema(name = "KbFileTree")
public class KbFileTree extends BaseEntity {

    @Schema(title = "知识库ID")
    private String kbId;

    @Schema(title = "父目录ID（null=根目录）")
    private String parentId;

    @Schema(title = "节点名称")
    private String name;

    @Schema(title = "类型: FOLDER=目录, FILE=文件")
    private String type;

    @Schema(title = "关联的文件ID（type=FILE时）")
    private String fileId;

    @Schema(title = "排序顺序")
    private Integer sortOrder;

    @Schema(title = "文件对象")
    @RelationOneToOne(selfField = "fileId", targetField = "id")
    private SysFile sysFile;

    /**
     * 覆盖父类的 delFlag 属性
     * 显式指定 isLogicDelete = false，此时对该实体的 delete 操作将变为物理删除
     */
    @Column(isLogicDelete = false)
    @JsonIgnore
    @ExcelIgnore
    @Schema(hidden = true)
    private Integer delFlag;
}
