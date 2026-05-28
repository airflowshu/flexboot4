package com.yunlbd.flexboot4.vo.ops;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictTypeListVO extends BaseAuditVO {
    private String code;
    private String name;
    private Integer status;
    private Integer orderNo;
}
