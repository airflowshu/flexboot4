package com.yunlbd.flexboot4.dto.ops;

import lombok.Data;

@Data
public class SysDictItemUpdateReq {
    private String typeId;
    private String itemCode;
    private String itemText;
    private String itemValue;
    private Integer status;
    private Integer orderNo;
    private String parentCode;
    private String remark;
}
