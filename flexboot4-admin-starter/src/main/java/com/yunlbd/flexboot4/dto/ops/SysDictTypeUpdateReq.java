package com.yunlbd.flexboot4.dto.ops;

import lombok.Data;

@Data
public class SysDictTypeUpdateReq {
    private String code;
    private String name;
    private Integer status;
    private Integer orderNo;
    private String remark;
}
