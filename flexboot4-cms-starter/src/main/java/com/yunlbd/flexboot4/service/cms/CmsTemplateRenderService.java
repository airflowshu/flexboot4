package com.yunlbd.flexboot4.service.cms;

import com.yunlbd.flexboot4.entity.cms.CmsArticle;

import java.nio.file.Path;

public interface CmsTemplateRenderService {

    RenderResult renderArticle(CmsArticle article);

    record RenderResult(String articleId, Path outputFile, String relativeUrl) {
    }
}

