package com.yunlbd.flexboot4.controller.cms;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.dto.cms.CmsArticleTagBindReq;
import com.yunlbd.flexboot4.entity.cms.CmsArticleTag;
import com.yunlbd.flexboot4.service.cms.CmsArticleTagService;
import com.yunlbd.flexboot4.vo.cms.CmsArticleTagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cms/article-tag")
@Tag(name = "文章标签关联", description = "CmsArticleTag - CMS文章标签关联管理")
@ApiTagGroup(group = "内容管理")
public class CmsArticleTagController {

    private final CmsArticleTagService service;

    public CmsArticleTagController(CmsArticleTagService service) {
        this.service = service;
    }

    @Operation(summary = "查询文章标签关联")
    @RequirePermission("cms:article:list")
    @PostMapping("/list")
    public ApiResult<List<CmsArticleTagVO>> list(@RequestBody SearchDto searchDto) {
        QueryWrapper queryWrapper = com.yunlbd.flexboot4.query.DefaultQueryWrapperBuilder.get()
                .build(searchDto == null ? new SearchDto() : searchDto, CmsArticleTag.class);
        return ApiResult.success(service.list(queryWrapper).stream().map(this::toVO).toList());
    }

    @Operation(summary = "绑定文章标签")
    @OperLog(title = "绑定文章标签", businessType = BusinessType.INSERT)
    @RequirePermission("cms:article:edit")
    @PostMapping
    public ApiResult<Boolean> bind(@RequestBody CmsArticleTagBindReq request) {
        CmsArticleTag entity = new CmsArticleTag();
        entity.setArticleId(request.getArticleId());
        entity.setTagId(request.getTagId());
        entity.setRemark(request.getRemark());
        return ApiResult.success(service.save(entity));
    }

    @Operation(summary = "解绑文章标签")
    @OperLog(title = "解绑文章标签", businessType = BusinessType.DELETE)
    @RequirePermission("cms:article:edit")
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> unbind(@PathVariable String id) {
        return ApiResult.success(service.removeById(id));
    }

    private CmsArticleTagVO toVO(CmsArticleTag entity) {
        CmsArticleTagVO vo = new CmsArticleTagVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setLastModifyTime(entity.getLastModifyTime());
        vo.setCreateBy(entity.getCreateBy());
        vo.setLastModifyBy(entity.getLastModifyBy());
        vo.setArticleId(entity.getArticleId());
        vo.setTagId(entity.getTagId());
        return vo;
    }
}
