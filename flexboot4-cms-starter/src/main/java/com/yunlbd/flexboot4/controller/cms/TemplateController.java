package com.yunlbd.flexboot4.controller.cms;

import com.mybatisflex.core.paginate.Page;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.entity.cms.CmsTemplatePublishRecord;
import com.yunlbd.flexboot4.service.cms.CmsTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cms/template")
@Tag(name = "模板管理", description = "TemplateController - CMS 模板管理")
@ApiTagGroup(group = "内容管理")
@RequiredArgsConstructor
public class TemplateController {

    private final CmsTemplateService cmsTemplateService;

    @GetMapping("/tree")
    @Operation(summary = "读取模板树", description = "递归读取模板根目录下的 HTML 文件树")
    @OperLog(title = "读取模板树", businessType = BusinessType.QUERY, isSaveResponseData = false)
    @RequirePermission("cms:template:view")
    public ApiResult<List<CmsTemplateService.TemplateTreeNode>> tree() {
        return ApiResult.success(cmsTemplateService.getTemplateTree());
    }

    @GetMapping("/file")
    @Operation(summary = "读取模板文件", description = "返回模板源码和预览用 HTML")
    @OperLog(title = "读取模板文件", businessType = BusinessType.QUERY, isSaveResponseData = false)
    @RequirePermission("cms:template:view")
    public ApiResult<CmsTemplateService.TemplateFileDetail> file(@RequestParam("path") String path) {
        return ApiResult.success(cmsTemplateService.getTemplateFile(path));
    }

    @PutMapping("/file")
    @Operation(summary = "保存模板文件", description = "直接回写模板 HTML 文件")
    @OperLog(title = "保存模板文件", businessType = BusinessType.UPDATE, isSaveResponseData = false)
    @RequirePermission("cms:template:edit")
    public ApiResult<Boolean> save(@RequestBody TemplateSaveRequest request) {
        return ApiResult.success(cmsTemplateService.saveTemplateFile(request.path(), request.content()));
    }

    @PostMapping("/publish")
    @Operation(summary = "发布模板站点", description = "将当前模板目录复制到发布目录并生成 ZIP")
    @OperLog(title = "发布模板站点", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @RequirePermission("cms:template:publish")
    public ApiResult<CmsTemplateService.PublishResult> publish() {
        return ApiResult.success(cmsTemplateService.publishCurrentTemplate());
    }

    @GetMapping("/publish/history")
    @Operation(summary = "分页查询发布记录", description = "按创建时间倒序查询模板发布记录")
    @OperLog(title = "分页查询发布记录", businessType = BusinessType.QUERY, isSaveResponseData = false)
    @RequirePermission("cms:template:view")
    public ApiResult<Page<CmsTemplatePublishRecord>> publishHistory(
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        return ApiResult.success(cmsTemplateService.pagePublishHistory(pageNumber, pageSize));
    }

    public record TemplateSaveRequest(String path, String content) {
    }
}
