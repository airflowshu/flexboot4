package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.BaseEntity;
import java.io.Serializable;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

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
@Table("sys_dict_item")
public class SysDictItem extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String typeCode;

    private String itemCode;

    private String itemText;

    private String itemValue;

    private Integer status;

    private Integer orderNo;

    private String parentCode;

}
