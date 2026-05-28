package com.yunlbd.flexboot4.vo.cms;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import com.yunlbd.flexboot4.vo.sys.SysFileSimpleVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsCategoryListVO extends BaseAuditVO {
    private String parentId;
    private String categoryName;
    private String categoryCode;
    private String description;
    private String coverFileId;
    private Integer sortOrder;
    private Integer status;
    private SysFileSimpleVO coverFile;
}
