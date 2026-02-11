package com.yunlbd.flexboot4.entity.ops;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 管理 API Key 实体类。
 *
 * @author Wangts
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("ai_api_key")
@Schema(name = "AiApiKey")
public class AiApiKey extends BaseEntity {


    /**
     * API Key 名称 / 标识
     */
    private String keyName;

    /**
     * e.g yl-xxxx
     */
    // @ColumnMask(Masks.ID_CARD_NUMBER)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "ai访问密钥") //只在入参中隐藏（响应中可见）
    private String apiKey;

    /**
     * 归属用户
     */
    private String userId;

    @ExcelIgnore //标明不需要导出
    @DictEnum("status")
    @Schema(example = "1", title = "状态值")
    private Integer status;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "状态") //只在入参中隐藏（响应中可见）
    @ExcelProperty("状态")
    @Column(ignore = true)
    private String statusStr;


    /**
     * 总额度
     */
    private Long quote;

    /**
     * 已使用量
     */
    private Long used;

    /**
     * 支持的模型列表 JSON
     */
    private String modelScope;

    /**
     * Key 过期时间，可为空
     */
    private LocalDateTime expiresAt;

    /**
     * 上次使用时间，可用于监控
     */
    private LocalDateTime lastUsedTime;

    /**
     * 可存放额外说明或策略信息
     */
    private String notes;

    @ExcelIgnore //标明不需要导出
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) //只在入参中隐藏（响应中可见）
    @RelationOneToOne(selfField = "userId", targetField = "id")
    private SysUser user;

}
