package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.converter.sys.SysFileCrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysFileCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysFileUpdateReq;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.excel.sys.SysFileExportRow;
import com.yunlbd.flexboot4.excel.sys.SysFileImportRow;
import com.yunlbd.flexboot4.file.FileAccessDescriptor;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import com.yunlbd.flexboot4.service.sys.SysFileService;
import com.yunlbd.flexboot4.storage.FileAccessTokenService;
import com.yunlbd.flexboot4.vo.sys.SysFileDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysFileListVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/admin/file")
@Tag(name = "文件管理", description = "SysFile - 文件管理")
@ApiTagGroup(group = "运维管理")
public class SysFileController extends BaseCrudController<SysFileService, SysFile, String,
        SysFileCreateReq, SysFileUpdateReq, SysFileListVO, SysFileDetailVO> {

    private final FileManagerService fileManagerService;
    private final SysFileCrudMapper mapper;
    private final FileAccessTokenService tokenService;

    public SysFileController(SysFileService service,
                             SysFileCrudMapper mapper,
                             FileManagerService fileManagerService,
                             FileAccessTokenService tokenService) {
        super(service, mapper);
        this.mapper = mapper;
        this.fileManagerService = fileManagerService;
        this.tokenService = tokenService;
    }


    @Override
    public Class<SysFile> getEntityClass() {
        return SysFile.class;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.same(List.of(
                "id", "tenantId", "bizType", "bizId", "fileName", "fileExt",
                "mimeType", "fileSize", "fileHash", "storageType", "bucketName",
                "objectKey", "aiStatus", "aiParseStatus", "aiEmbedStatus",
                "chunkCount", "tokenEstimate", "embeddingModel", "remark",
                "createTime", "lastModifyTime"
        ));
    }

    @Override
    protected CrudExcelSupport<SysFile, ?, ?> excelSupport() {
        return CrudExcelSupport.of(SysFileExportRow.class, SysFileImportRow.class, mapper::toExportRow, null);
    }

    /**
     * 重写单删除：先清除缓存和文件，再删除数据库记录
     */
    @Override
    @Operation(summary = "删除文件", description = "删除文件并清除相关缓存")
    @OperLog(title = "删除文件", businessType = BusinessType.DELETE)
    @RequirePermission("sys:file:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> remove(@PathVariable String id) {
        fileManagerService.delete(id);
        return ApiResult.success(service.removeById(id));
    }

    /**
     * 重写批量删除：先清除缓存和文件，再删除数据库记录
     */
    @Override
    @Operation(summary = "批量删除文件", description = "批量删除文件并清除相关缓存")
    @OperLog(title = "批量删除文件", businessType = BusinessType.DELETE)
    @RequirePermission("sys:file:delete")
    @DeleteMapping
    public ApiResult<Boolean> removeBatch(@RequestBody Collection<String> ids) {
        ids.forEach(fileManagerService::delete);
        return ApiResult.success(service.removeByIds(ids));
    }

    @Operation(summary = "上传单文件", description = "上传单个文件并保存到对象存储")
    @OperLog(title = "上传单文件", businessType = BusinessType.UPLOAD)
    @RequirePermission("sys:file:upload")
    @PostMapping(value = "/upload-single",produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<FileObject> uploadSingle(
            @Parameter(description = "要上传的文件", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "租户ID") @RequestParam(value = "tenantId", defaultValue = "1") String tenantId,
            @Parameter(description = "业务类型，传入 `sys_user_avatar` 时文件会上传到公有库，其他值存入私有库")
            @RequestParam(value = "bizType", required = false) String bizType,
            @Parameter(description = "业务ID") @RequestParam(value = "bizId", required = false) String bizId) {
        FileObject obj = fileManagerService.upload(file, tenantId, bizType, bizId);
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

    @Operation(summary = "访问本地存储文件", description = "使用短期签名令牌访问本地存储文件")
    @RequirePermission(skip = true)
    @GetMapping({"/access/{token}", "/access/{token}/{fileName:.+}"})
    public void accessLocalFile(@PathVariable String token, HttpServletRequest request, HttpServletResponse response) {
        FileAccessTokenService.AccessToken accessToken;
        try {
            accessToken = tokenService.verify(token);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e);
        }

        SysFile file = service.getById(accessToken.fileId());
        if (file == null || file.getDelFlag() != null && file.getDelFlag() != 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
        }

        long length = file.getFileSize() == null ? -1L : file.getFileSize();
        response.setContentType(file.getMimeType() == null || file.getMimeType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : file.getMimeType());
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition", contentDisposition(file.getFileName(), accessToken.attachment()));

        try (InputStream in = fileManagerService.load(accessToken.fileId())) {
            long[] range = parseRange(request.getHeader("Range"), length);
            if (range == null) {
                if (length >= 0) {
                    response.setContentLengthLong(length);
                }
                in.transferTo(response.getOutputStream());
                return;
            }

            long start = range[0];
            long end = range[1];
            long contentLength = end - start + 1;
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + length);
            response.setContentLengthLong(contentLength);
            skipFully(in, start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read < 0) {
                    break;
                }
                response.getOutputStream().write(buffer, 0, read);
                remaining -= read;
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "file access failed", e);
        }
    }

    private String contentDisposition(String fileName, boolean attachment) {
        String safeName = fileName == null || fileName.isBlank() ? "file" : fileName;
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        return (attachment ? "attachment" : "inline") + "; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;
    }

    private long[] parseRange(String range, long length) {
        if (range == null || range.isBlank() || length < 0 || !range.startsWith("bytes=")) {
            return null;
        }
        String value = range.substring("bytes=".length()).trim();
        if (value.contains(",")) {
            return null;
        }
        String[] parts = value.split("-", 2);
        try {
            long start;
            long end;
            if (parts[0].isBlank()) {
                long suffix = Long.parseLong(parts[1]);
                if (suffix <= 0) {
                    return null;
                }
                start = Math.max(0, length - suffix);
                end = length - 1;
            } else {
                start = Long.parseLong(parts[0]);
                end = parts.length > 1 && !parts[1].isBlank() ? Long.parseLong(parts[1]) : length - 1;
            }
            if (start < 0 || end < start || start >= length) {
                return null;
            }
            return new long[]{start, Math.min(end, length - 1)};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void skipFully(InputStream in, long bytes) throws java.io.IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped <= 0) {
                if (in.read() < 0) {
                    break;
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}
