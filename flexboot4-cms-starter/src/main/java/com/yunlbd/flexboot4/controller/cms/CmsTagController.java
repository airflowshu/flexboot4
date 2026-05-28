package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.cms.CmsTagCreateReq;
import com.yunlbd.flexboot4.dto.cms.CmsTagUpdateReq;
import com.yunlbd.flexboot4.entity.cms.CmsTag;
import com.yunlbd.flexboot4.service.cms.CmsTagService;
import com.yunlbd.flexboot4.vo.cms.CmsTagDetailVO;
import com.yunlbd.flexboot4.vo.cms.CmsTagListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/tag")
@Tag(name = "标签管理", description = "CmsTag - CMS标签管理")
@ApiTagGroup(group = "内容管理")
public class CmsTagController extends EntityCrudController<CmsTagService, CmsTag, String,
        CmsTagCreateReq, CmsTagUpdateReq, CmsTagListVO, CmsTagDetailVO> {

    public CmsTagController(CmsTagService service) {
        super(service, CmsTag.class, CmsTagListVO.class, CmsTagDetailVO.class);
    }


    @Override
    public Class<CmsTag> getEntityClass() {
        return CmsTag.class;
    }
}

