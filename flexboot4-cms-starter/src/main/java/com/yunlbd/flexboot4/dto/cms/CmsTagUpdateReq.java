package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

@Data
public class CmsTagUpdateReq {
    private String tagName;
    private String tagColor;
    private Integer useCount;
    private String remark;
}
