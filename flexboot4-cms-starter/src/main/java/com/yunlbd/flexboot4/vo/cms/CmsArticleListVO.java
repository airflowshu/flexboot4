package com.yunlbd.flexboot4.vo.cms;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import com.yunlbd.flexboot4.vo.sys.SysFileSimpleVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsArticleListVO extends BaseAuditVO {
    private String title;
    private String categoryId;
    private String author;
    private String coverFileId;
    private String summary;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private String status;
    private String statusStr;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private String reviewerId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    private String reviewComment;
    private Integer sortOrder;
    private CmsCategoryListVO category;
    private SysFileSimpleVO coverFile;
    private List<CmsTagListVO> tags;
}
