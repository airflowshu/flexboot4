package com.yunlbd.flexboot4.vo.cms;

import com.yunlbd.flexboot4.vo.sys.BaseAuditVO;
import com.yunlbd.flexboot4.vo.sys.SysFileSimpleVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CmsArticleFileListVO extends BaseAuditVO {
    private String articleId;
    private String fileId;
    private Integer sortOrder;
    private CmsArticleListVO article;
    private SysFileSimpleVO file;
}
