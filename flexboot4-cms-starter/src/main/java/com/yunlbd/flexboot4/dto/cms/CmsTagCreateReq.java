package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

@Data
public class CmsTagCreateReq {
    private String tagName;
    private String tagColor;
    private Integer useCount;
    private String remark;
}
