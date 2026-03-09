package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.cms.CmsTag;
import com.yunlbd.flexboot4.service.cms.CmsTagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/tag")
@RequiredArgsConstructor
@Tag(name = "标签管理", description = "CmsTag - CMS标签管理")
@ApiTagGroup(group = "内容管理")
public class CmsTagController extends BaseController<CmsTagService, CmsTag, String> {

    @Override
    public Class<CmsTag> getEntityClass() {
        return CmsTag.class;
    }
}

