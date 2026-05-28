package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

@Data
public class CmsArticleFileUpdateReq {
    private String articleId;
    private String fileId;
    private Integer sortOrder;
    private String remark;
}
