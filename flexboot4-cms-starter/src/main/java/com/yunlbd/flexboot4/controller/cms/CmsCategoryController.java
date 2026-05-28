package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.cms.CmsCategoryCreateReq;
import com.yunlbd.flexboot4.dto.cms.CmsCategoryUpdateReq;
import com.yunlbd.flexboot4.entity.cms.CmsCategory;
import com.yunlbd.flexboot4.service.cms.CmsCategoryService;
import com.yunlbd.flexboot4.vo.cms.CmsCategoryDetailVO;
import com.yunlbd.flexboot4.vo.cms.CmsCategoryListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/category")
@Tag(name = "栏目管理", description = "CmsCategory - CMS栏目管理")
@ApiTagGroup(group = "内容管理")
public class CmsCategoryController extends EntityCrudController<CmsCategoryService, CmsCategory, String,
        CmsCategoryCreateReq, CmsCategoryUpdateReq, CmsCategoryListVO, CmsCategoryDetailVO> {

    public CmsCategoryController(CmsCategoryService service) {
        super(service, CmsCategory.class, CmsCategoryListVO.class, CmsCategoryDetailVO.class);
    }


    @Override
    public Class<CmsCategory> getEntityClass() {
        return CmsCategory.class;
    }
}

