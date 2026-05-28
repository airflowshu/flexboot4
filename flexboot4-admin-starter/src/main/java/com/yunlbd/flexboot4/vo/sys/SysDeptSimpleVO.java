package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;

@Data
public class SysDeptSimpleVO {
    private String id;
    private String parentId;
    private String deptName;
    private Integer orderNo;
    private Integer status;
    private String remark;
}
