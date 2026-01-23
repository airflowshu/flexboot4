package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 *  实体类。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_dict_type")
@Schema(name = "SysDictType")
public class SysDictType extends BaseEntity{

    private String code;

    private String name;

    private Integer status;

    private Integer orderNo;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY) //只在入参中隐藏（响应中可见）
    @RelationOneToMany(selfField = "id", targetField = "typeId", orderBy = "order_no")
    private List<SysDictItem> dictItems;

}
