package com.yunlbd.flexboot4.controller.cms;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * CMS 专用文件上传接口
 * 自动设置 bizType=cms, bucketName=flexboot4-public
 */
@RestController
@RequestMapping("/api/admin/cms/file")
@Tag(name = "CMS文件上传", description = "CMS专用文件上传（公共桶）")
@ApiTagGroup(group = "内容管理")
@RequiredArgsConstructor
public class CmsFileController {

    private final FileManagerService fileManagerService;

    @Operation(summary = "CMS文件上传", description = "上传CMS文件到公共桶（自动设置bizType=cms, bucket=flexboot4-public）")
    @OperLog(title = "CMS文件上传", businessType = BusinessType.UPLOAD)
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<FileObject> upload(
            @Parameter(description = "要上传的文件", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", required = false, defaultValue = "1") String tenantId,
            @Parameter(description = "业务ID（如文章ID）") @RequestParam(value = "bizId", required = false) String bizId) {

        // 强制设置 bizType 为 cms，使用公共桶策略
        FileObject obj = fileManagerService.upload(file, tenantId, "cms", bizId);
        return ApiResult.success(obj);
    }
}

