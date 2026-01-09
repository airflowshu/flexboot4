package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.BaseEntity;
import java.io.Serializable;

import java.io.Serial;
import java.util.List;

import com.yunlbd.flexboot4.mapper.SysDictItemMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import javax.management.relation.RelationType;

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
public class SysDictType extends BaseEntity{

    private String code;

    private String name;

    private Integer status;

    private Integer orderNo;

    @RelationOneToMany(selfField = "code", targetField = "typeCode", orderBy = "order_no")
    private List<SysDictItem> dictItems;

}
