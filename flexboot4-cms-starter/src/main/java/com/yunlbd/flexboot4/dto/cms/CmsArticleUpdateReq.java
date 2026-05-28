package com.yunlbd.flexboot4.dto.cms;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CmsArticleUpdateReq {
    private String title;
    private String categoryId;
    private String author;
    private String coverFileId;
    private String summary;
    private String content;
    private String status;
    private LocalDateTime publishTime;
    private String reviewerId;
    private LocalDateTime reviewTime;
    private String reviewComment;
    private Integer sortOrder;
    private String remark;
}
