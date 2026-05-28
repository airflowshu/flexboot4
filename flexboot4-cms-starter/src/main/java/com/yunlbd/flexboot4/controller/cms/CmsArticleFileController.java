package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.cms.CmsArticleFileCreateReq;
import com.yunlbd.flexboot4.dto.cms.CmsArticleFileUpdateReq;
import com.yunlbd.flexboot4.entity.cms.CmsArticleFile;
import com.yunlbd.flexboot4.service.cms.CmsArticleFileService;
import com.yunlbd.flexboot4.vo.cms.CmsArticleFileDetailVO;
import com.yunlbd.flexboot4.vo.cms.CmsArticleFileListVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/cms/article-file")
@Tag(name = "文章附件管理", description = "CmsArticleFile - CMS文章附件管理")
@ApiTagGroup(group = "内容管理")
public class CmsArticleFileController extends EntityCrudController<CmsArticleFileService, CmsArticleFile, String,
        CmsArticleFileCreateReq, CmsArticleFileUpdateReq, CmsArticleFileListVO, CmsArticleFileDetailVO> {

    public CmsArticleFileController(CmsArticleFileService service) {
        super(service, CmsArticleFile.class, CmsArticleFileListVO.class, CmsArticleFileDetailVO.class);
    }


    @Override
    public Class<CmsArticleFile> getEntityClass() {
        return CmsArticleFile.class;
    }
}

