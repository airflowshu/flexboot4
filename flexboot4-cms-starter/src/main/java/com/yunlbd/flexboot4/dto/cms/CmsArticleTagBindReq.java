package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

@Data
public class CmsArticleTagBindReq {
    private String articleId;
    private String tagId;
    private String remark;
}
