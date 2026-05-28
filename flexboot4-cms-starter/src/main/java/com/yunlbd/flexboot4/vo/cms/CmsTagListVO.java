package com.yunlbd.flexboot4.vo.cms;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsTagListVO extends BaseAuditVO {
    private String tagName;
    private String tagColor;
    private Integer useCount;
}
