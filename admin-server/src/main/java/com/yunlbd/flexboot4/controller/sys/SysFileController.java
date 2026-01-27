package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.entity.SysFile;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.service.FileManagerService;
import com.yunlbd.flexboot4.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/file")
@Tag(name = "文件管理", description = "SysFile - 文件管理")
@RequiredArgsConstructor
public class SysFileController extends BaseController<SysFileService, SysFile, String> {

    private final FileManagerService fileManagerService;

    @Override
    public Class<SysFile> getEntityClass() {
        return SysFile.class;
    }

    @Operation(summary = "上传文件", description = "上传单个文件并保存到对象存储")
    @OperLog(title = "上传文件", businessType = BusinessType.UPLOAD)
    @RequirePermission("sys:file:upload")
    @PostMapping(value = "/upload",produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<FileObject> upload(
            @Parameter(description = "要上传的文件", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", defaultValue = "1") String tenantId,
            @Parameter(description = "项目ID") @RequestParam(value = "projectId", required = false) String projectId,
            @Parameter(description = "业务类型，传入 `sys_user_avatar` 时文件会上传到公有库，其他值存入私有库")
            @RequestParam(value = "bizType", required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(value = "bizId", required = false) String bizId) {
        FileObject obj = fileManagerService.upload(file, tenantId, projectId, bizType, bizId);
        return ApiResult.success(obj);
    }

    @Operation(summary = "获取文件访问地址", description = "生成文件的预签名访问 URL")
    @OperLog(title = "获取文件访问地址", businessType = BusinessType.OTHER)
    @RequirePermission("sys:file:download")
    @GetMapping("/{id}/access-url")
    public ApiResult<FileAccessDescriptor> accessUrl(@PathVariable("id") String id,
                                                     @RequestParam(value = "ttlSeconds", defaultValue = "600") long ttlSeconds,
                                                     @RequestParam(value = "attachment", defaultValue = "true") boolean attachment) {
        FileAccessDescriptor descriptor = fileManagerService.access(id, ttlSeconds, attachment);
        return ApiResult.success(descriptor);
    }
}
