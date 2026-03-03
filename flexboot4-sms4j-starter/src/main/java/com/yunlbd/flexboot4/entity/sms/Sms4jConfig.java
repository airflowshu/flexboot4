package com.yunlbd.flexboot4.entity.sms;

import com.alibaba.excel.annotation.ExcelProperty;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 短信厂商配置表
 * <p>
 * 对应 sms4j 接口配置自定义数据来源（第二种配置方式），支持动态多厂商管理。
 * 核心公共字段显式存列，各厂商差异化参数通过 ext_params (JSONB) 扩展存储。
 * </p>
 *
 * @author flexboot4
 * @since 2026-03-03
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sms4j_config")
@Schema(name = "Sms4jConfig", description = "短信厂商配置")
public class Sms4jConfig extends BaseEntity {

    @ExcelProperty("配置名称")
    @Schema(description = "配置名称，便于管理员识别，如：阿里云-主账号")
    private String configName;

    @ExcelProperty("厂商类型")
    @DictEnum("sms_supplier_type")
    @Schema(description = "厂商类型，如 alibaba / tencent / huawei / jdcloud / cloopen / yunpian 等")
    private String supplierType;

    @Schema(description = "厂商类型Str，如 阿里巴巴 / 腾讯 / 华为 / 京东云 等", accessMode = Schema.AccessMode.READ_ONLY)
    @Column(ignore = true)
    private String supplierTypeStr;

    @ExcelProperty("配置ID")
    @Schema(description = "sms4j 框架内部标识，同一厂商可配置多个，需全局唯一，新增时系统自动生成", accessMode = Schema.AccessMode.READ_ONLY)
    private String configId;

    @Schema(description = "AccessKeyId / AppId / 账号，敏感信息")
    private String accessKeyId;

    @Schema(description = "AccessKeySecret / AppKey / 密码，敏感信息")
    private String accessKeySecret;

    @ExcelProperty("短信签名")
    @Schema(description = "短信签名，如：【云途商城】")
    private String signature;

    @ExcelProperty("模板ID")
    @Schema(description = "默认短信模板 ID（部分厂商必填）")
    private String templateId;

    @ExcelProperty("SDK AppId")
    @Schema(description = "SDK AppId，腾讯云等厂商专用")
    private String sdkAppId;

    @ExcelProperty("权重")
    @Schema(description = "负载均衡权重，数值越大被选中的概率越高，默认 1", example = "1")
    private Integer weight;

    @ExcelProperty("是否默认")
    @Schema(description = "是否为默认配置：1-是，0-否")
    private Integer isDefault;

    @Schema(description = "厂商差异化扩展参数，JSON 格式，存储各厂商特有字段")
    private String extParams;

    @ExcelProperty("状态")
    @DictEnum("status")
    @Schema(description = "状态：1-启用，0-禁用", example = "1")
    private Integer status;

    @Column(ignore = true)
    @Schema(description = "状态描述", accessMode = Schema.AccessMode.READ_ONLY)
    private String statusStr;

}

