package com.yunlbd.flexboot4.entity.sys;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.core.keygen.KeyGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "主键id")
    private String id;

    @ExcelIgnore
    // @Column(version = true)  暂不启用乐观锁
    @Schema(hidden = true)
    private Long version;

    @JsonIgnore
    @ExcelIgnore
    @Column(isLogicDelete = true)
    @Schema(hidden = true)
    private Integer delFlag;

    @ExcelProperty("创建时间")
    @Column(onInsertValue = "now()")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("更新时间")
    @Column(onUpdateValue = "now()", onInsertValue = "now()")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "更新时间")
    private LocalDateTime lastModifyTime;

    @ExcelProperty("创建人")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "创建人")
    private String createBy;

    @ExcelProperty("更新人")
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "更新人")
    private String lastModifyBy;

    @ExcelProperty("备注")
    @Schema(title = "备注")
    private String remark;
}
