package com.yunlbd.flexboot4.controller.cms;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.dto.cms.CmsArticleCreateReq;
import com.yunlbd.flexboot4.dto.cms.CmsArticleUpdateReq;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.service.cms.CmsArticleService;
import com.yunlbd.flexboot4.vo.cms.CmsArticleDetailVO;
import com.yunlbd.flexboot4.vo.cms.CmsArticleListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/cms/article")
@Tag(name = "文章管理", description = "CmsArticle - CMS文章管理")
@ApiTagGroup(group = "内容管理")
public class CmsArticleController extends EntityCrudController<CmsArticleService, CmsArticle, String,
        CmsArticleCreateReq, CmsArticleUpdateReq, CmsArticleListVO, CmsArticleDetailVO> {

    public CmsArticleController(CmsArticleService service) {
        super(service, CmsArticle.class, CmsArticleListVO.class, CmsArticleDetailVO.class);
    }


    @Override
    public Class<CmsArticle> getEntityClass() {
        return CmsArticle.class;
    }

    @Override
    @Operation(summary = "分页查询文章列表", description = "根据用户角色进行数据权限过滤：管理员可见所有文章，普通用户只能看到自己创建的文章")
    @OperLog(businessType = BusinessType.QUERY, isSaveResponseData = false)
    @PostMapping("/page")
    public ApiResult<Page<CmsArticleListVO>> page(@RequestBody SearchDto searchDto) {
        Page<CmsArticle> result = service.pageWithPermissionFilter(searchDto);
        Page<CmsArticleListVO> voPage = new Page<>(result.getPageNumber(), result.getPageSize());
        voPage.setTotalRow(result.getTotalRow());
        voPage.setRecords(crudMapper.toListVOList(result.getRecords()));
        return ApiResult.success(voPage);
    }

    @Operation(summary = "提交文章审核", description = "将草稿或被驳回的文章提交审核")
    @OperLog(title = "提交文章审核", businessType = BusinessType.UPDATE)
    @RequirePermission("cms:article:submit")
    @PostMapping("/{id}/submit")
    public ApiResult<Boolean> submitForReview(@PathVariable String id) {
        return ApiResult.success(service.submitForReview(id));
    }

    @Operation(summary = "审核通过", description = "管理员审核通过文章")
    @OperLog(title = "审核通过文章", businessType = BusinessType.UPDATE)
    @RequirePermission("cms:article:review")
    @PostMapping("/{id}/approve")
    public ApiResult<Boolean> approveArticle(
            @PathVariable String id,
            @RequestBody(required = false) ReviewRequest request) {
        String comment = request != null ? request.reviewComment() : null;
        return ApiResult.success(service.approveArticle(id, comment));
    }

    @Operation(summary = "驳回文章", description = "管理员驳回文章")
    @OperLog(title = "驳回文章", businessType = BusinessType.UPDATE)
    @RequirePermission("cms:article:review")
    @PostMapping("/{id}/reject")
    public ApiResult<Boolean> rejectArticle(
            @PathVariable String id,
            @RequestBody(required = false) ReviewRequest request) {
        String comment = request != null ? request.reviewComment() : null;
        return ApiResult.success(service.rejectArticle(id, comment));
    }

    @Operation(summary = "增加浏览量", description = "文章浏览量+1")
    @OperLog(title = "增加文章浏览量", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    @RequirePermission("cms:article:list")
    @PostMapping("/{id}/view")
    public ApiResult<Boolean> incrementViewCount(@PathVariable String id) {
        return ApiResult.success(service.incrementViewCount(id));
    }

    @Operation(summary = "生成文章预览页", description = "按模板生成静态 HTML 预览页，返回可直接打开的 URL")
    @OperLog(title = "生成文章预览页", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @RequirePermission("cms:article:preview")
    @PostMapping("/{id}/preview")
    public ApiResult<PreviewPageResponse> previewArticle(@PathVariable String id) {
        String relativeUrl = service.renderPreviewPage(id);
        String previewUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path(relativeUrl).toUriString();
        return ApiResult.success(new PreviewPageResponse(relativeUrl, previewUrl));
    }

    public record ReviewRequest(String reviewComment) {}

    public record PreviewPageResponse(String relativeUrl, String previewUrl) {}
}
