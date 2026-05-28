package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

@Data
public class CmsCategoryCreateReq {
    private String parentId;
    private String categoryName;
    private String categoryCode;
    private String description;
    private String coverFileId;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
