package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.handler.JacksonTypeHandler;
import com.yunlbd.flexboot4.mybatis.typehandler.JsonbTypeHandler;
import com.yunlbd.flexboot4.common.annotation.DictEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_oper_log")
@Schema(name = "SysOperLog", description = "操作日志")
public class SysOperLog extends BaseEntity {

    @Schema(title = "模块标题")
    private String title;

    @DictEnum("businessType")
    @Schema(title = "业务类型（0其它 1新增 2修改 3删除 4调用api 5导出 6导入 7查询 8登录 9登出）")
    private Integer businessType;

    @Schema(title = "业务类型Str（0其它 1新增 2修改 3删除 4调用api 5导出 6导入 7查询 8登录 9登出）")
    @Column(ignore = true)
    private String businessTypeStr;

    @Schema(title = "方法名称")
    private String method;

    @Schema(title = "请求方式")
    private String requestMethod;

    @DictEnum("operatorType")
    @Schema(title = "操作类别（0其它 1后台用户 2移动端用户）")
    private Integer operatorType;

    @Schema(title = "操作类别（0其它 1后台用户 2移动端用户）")
    @Column(ignore = true)
    private String operatorTypeStr;

    @Schema(title = "操作人员")
    private String operName;

    @Schema(title = "操作人员id")
    private String operUserId;

    @Schema(title = "部门名称")
    private String deptName;

    @Schema(title = "请求URL")
    private String operUrl;

    @Schema(title = "主机地址")
    private String operIp;

    @Schema(title = "操作地点")
    private String operLocation;

    @Schema(title = "请求参数")
    @Column(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> operParam;

    @Schema(title = "返回参数")
    @Column(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> jsonResult;

    @Schema(title = "操作状态（0正常 1异常）")
    private Integer status;

    @Schema(title = "错误消息")
    private String errorMsg;

    @Schema(title = "操作时间")
    private LocalDateTime operTime;

    @Schema(title = "消耗时间")
    private Long costTime;
    
    @Schema(title = "扩展参数")
    @Column(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> extParams;
}
