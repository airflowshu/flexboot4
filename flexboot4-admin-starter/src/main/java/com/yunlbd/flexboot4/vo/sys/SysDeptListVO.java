package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDeptListVO extends BaseAuditVO {
    private String deptName;
    private Integer orderNo;
    private Integer status;
    private String statusStr;
    private String parentId;
}
