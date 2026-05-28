package com.yunlbd.flexboot4.dto.sys;

import lombok.Data;

@Data
public class SysDeptUpdateReq {
    private String deptName;
    private Integer orderNo;
    private Integer status;
    private String parentId;
    private String remark;
}
