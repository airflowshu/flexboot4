package com.yunlbd.flexboot4.service.cms;

import com.yunlbd.flexboot4.entity.cms.CmsArticle;

public interface CmsContentSanitizer {

    void sanitizeForPersistence(CmsArticle article);
}

