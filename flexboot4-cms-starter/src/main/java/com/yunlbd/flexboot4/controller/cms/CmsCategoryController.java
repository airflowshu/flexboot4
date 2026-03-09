package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.cms.CmsCategory;
import com.yunlbd.flexboot4.service.cms.CmsCategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/category")
@RequiredArgsConstructor
@Tag(name = "栏目管理", description = "CmsCategory - CMS栏目管理")
@ApiTagGroup(group = "内容管理")
public class CmsCategoryController extends BaseController<CmsCategoryService, CmsCategory, String> {

    @Override
    public Class<CmsCategory> getEntityClass() {
        return CmsCategory.class;
    }
}

