package com.yunlbd.flexboot4.controller.cms;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.cms.CmsArticle;
import com.yunlbd.flexboot4.service.cms.CmsArticleService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cms/article")
@RequiredArgsConstructor
@Tag(name = "文章管理", description = "CmsArticle - CMS文章管理")
@ApiTagGroup(group = "内容管理")
public class CmsArticleController extends BaseController<CmsArticleService, CmsArticle, String> {

    @Override
    public Class<CmsArticle> getEntityClass() {
        return CmsArticle.class;
    }

    @Override
    @Operation(summary = "分页查询文章列表", description = "根据用户角色进行数据权限过滤：管理员可见所有文章，普通用户只能看到自己创建的文章")
    @OperLog(businessType = BusinessType.QUERY, isSaveResponseData = false)
    @PostMapping("/page")
    public ApiResult<Page<CmsArticle>> page(@RequestBody SearchDto searchDto) {
        Page<CmsArticle> result = service.pageWithPermissionFilter(searchDto);
        return ApiResult.success(result);
    }

    @Operation(summary = "提交文章审核", description = "将草稿或被驳回的文章提交审核")
    @OperLog(title = "提交文章审核", businessType = BusinessType.UPDATE)
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
    @PostMapping("/{id}/view")
    public ApiResult<Boolean> incrementViewCount(@PathVariable String id) {
        return ApiResult.success(service.incrementViewCount(id));
    }

    public record ReviewRequest(String reviewComment) {}
}

