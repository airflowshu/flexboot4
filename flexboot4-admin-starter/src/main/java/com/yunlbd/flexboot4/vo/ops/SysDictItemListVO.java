package com.yunlbd.flexboot4.vo.ops;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictItemListVO extends BaseAuditVO {
    private String typeId;
    private String itemCode;
    private String itemText;
    private String itemValue;
    private Integer status;
    private Integer orderNo;
    private String parentCode;
}
