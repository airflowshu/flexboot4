package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.cms.CmsArticleFile;
import com.yunlbd.flexboot4.service.cms.CmsArticleFileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/article-file")
@RequiredArgsConstructor
@Tag(name = "文章附件管理", description = "CmsArticleFile - CMS文章附件管理")
@ApiTagGroup(group = "内容管理")
public class CmsArticleFileController extends BaseController<CmsArticleFileService, CmsArticleFile, String> {

    @Override
    public Class<CmsArticleFile> getEntityClass() {
        return CmsArticleFile.class;
    }
}

