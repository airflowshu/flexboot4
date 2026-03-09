package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.cms.CmsArticleTag;
import com.yunlbd.flexboot4.service.cms.CmsArticleTagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/article-tag")
@RequiredArgsConstructor
@Tag(name = "文章标签关联", description = "CmsArticleTag - CMS文章标签关联管理")
@ApiTagGroup(group = "内容管理")
public class CmsArticleTagController extends BaseController<CmsArticleTagService, CmsArticleTag, String> {

    @Override
    public Class<CmsArticleTag> getEntityClass() {
        return CmsArticleTag.class;
    }
}

