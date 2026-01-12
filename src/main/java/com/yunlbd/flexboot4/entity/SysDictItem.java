package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
public class SysDictItem extends BaseEntity {

    private String typeCode;

    private String itemCode;

    private String itemText;

    private String itemValue;

    private Integer status;

    private Integer orderNo;

    private String parentCode;

}
