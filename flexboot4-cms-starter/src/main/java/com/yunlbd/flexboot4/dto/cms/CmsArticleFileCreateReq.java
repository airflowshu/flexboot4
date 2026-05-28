package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

@Data
public class CmsArticleFileCreateReq {
    private String articleId;
    private String fileId;
    private Integer sortOrder;
    private String remark;
}
