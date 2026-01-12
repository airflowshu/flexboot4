package com.yunlbd.flexboot4.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    @ExcelIgnore
    private String id;

    @ExcelIgnore
    @Column(version = true)
    private Long version;

    @JsonIgnore
    @ExcelIgnore
    @Column(isLogicDelete = true)
    private Integer delFlag;

    @ExcelProperty("创建时间")
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @ExcelProperty("更新时间")
    @Column(onUpdateValue = "now()", onInsertValue = "now()")
    private LocalDateTime lastModifyTime;

    @ExcelProperty("创建人")
    private String createBy;

    @ExcelProperty("更新人")
    private String lastModifyBy;

    @ExcelProperty("备注")
    private String remark;
}
