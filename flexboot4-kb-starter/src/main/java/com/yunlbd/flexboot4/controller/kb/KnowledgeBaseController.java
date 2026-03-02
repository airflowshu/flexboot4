package com.yunlbd.flexboot4.controller.kb;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.kb.KbFileTree;
import com.yunlbd.flexboot4.entity.kb.KbMember;
import com.yunlbd.flexboot4.entity.kb.KnowledgeBase;
import com.yunlbd.flexboot4.file.FileObject;
import com.yunlbd.flexboot4.service.kb.KbFileTreeService;
import com.yunlbd.flexboot4.service.kb.KbMemberService;
import com.yunlbd.flexboot4.service.kb.KnowledgeBaseIndexingService;
import com.yunlbd.flexboot4.service.kb.KnowledgeBaseService;
import com.yunlbd.flexboot4.service.sys.FileManagerService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/admin/kb")
@Tag(name = "知识库管理", description = "知识库 - 成员/文件/目录")
@ApiTagGroup(group = "知识库管理")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbMemberService kbMemberService;
    private final KbFileTreeService kbFileTreeService;
    private final KnowledgeBaseIndexingService knowledgeBaseIndexingService;
    private final FileManagerService fileManagerService;

    @Operation(summary = "创建知识库")
    @OperLog(title = "创建知识库", businessType = BusinessType.INSERT)
    // @RequirePermission("kb:manage:create")
    @PostMapping
    public ApiResult<Boolean> create(@RequestBody KnowledgeBase kb) {
        String userId = SecurityUtils.getUserId();
        return ApiResult.success(knowledgeBaseService.createKnowledgeBase(kb, userId));
    }

    @Operation(summary = "更新知识库")
    @OperLog(title = "更新知识库", businessType = BusinessType.UPDATE)
    // @RequirePermission("kb:manage:update")
    @PutMapping("/{id}")
    public ApiResult<Boolean> update(@PathVariable String id, @RequestBody KnowledgeBase kb) {
        String userId = SecurityUtils.getUserId();
        return ApiResult.success(knowledgeBaseService.updateKnowledgeBase(id, kb, userId));
    }

    @Operation(summary = "删除知识库")
    @OperLog(title = "删除知识库", businessType = BusinessType.DELETE)
    // @RequirePermission("kb:manage:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> delete(@PathVariable String id) {
        String userId = SecurityUtils.getUserId();
        return ApiResult.success(knowledgeBaseService.deleteKnowledgeBase(id, userId));
    }

    @Operation(summary = "知识库详情")
    @OperLog(title = "知识库详情", businessType = BusinessType.QUERY, isSaveResponseData = false)
    // @RequirePermission("kb:manage:view")
    @GetMapping("/{id}")
    public ApiResult<KnowledgeBase> get(@PathVariable String id) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkVisible(id, userId);
        return ApiResult.success(knowledgeBaseService.getById(id));
    }

    @Operation(summary = "知识库分页")
    @OperLog(title = "知识库分页", businessType = BusinessType.QUERY, isSaveResponseData = false)
    // @RequirePermission("kb:manage:view")
    @PostMapping("/page")
    public ApiResult<?> page(@RequestBody SearchDto searchDto) {
        String userId = SecurityUtils.getUserId();
        return ApiResult.success(knowledgeBaseService.pageVisible(searchDto, userId));
    }

    @Operation(summary = "知识库列表")
    @OperLog(title = "知识库列表", businessType = BusinessType.QUERY, isSaveResponseData = false)
    // @RequirePermission("kb:manage:view")
    @PostMapping("/list")
    public ApiResult<List<KnowledgeBase>> list(@RequestBody SearchDto searchDto) {
        String userId = SecurityUtils.getUserId();
        return ApiResult.success(knowledgeBaseService.pageVisible(searchDto, userId).getRecords());
    }

    // ========== 成员管理 ==========

    @Operation(summary = "列出知识库成员")
    @OperLog(title = "列出知识库成员", businessType = BusinessType.QUERY, isSaveResponseData = false)
    // @RequirePermission("kb:member:view")
    @GetMapping("/{kbId}/members")
    public ApiResult<List<KbMember>> listMembers(@PathVariable String kbId) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkVisible(kbId, userId);
        KnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (!"team".equalsIgnoreCase(kb.getType())) {
            return ApiResult.success(List.of());
        }
        return ApiResult.success(kbMemberService.listByKbId(kbId));
    }

    @Operation(summary = "添加知识库成员")
    @OperLog(title = "添加知识库成员", businessType = BusinessType.INSERT)
    @RequirePermission("kb:member:add")
    // @PostMapping("/{kbId}/members")
    public ApiResult<Boolean> addMembers(@PathVariable String kbId, @RequestBody Collection<String> userIds) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        return ApiResult.success(kbMemberService.addMembers(kbId, userIds));
    }

    @Operation(summary = "移除知识库成员")
    @OperLog(title = "移除知识库成员", businessType = BusinessType.DELETE)
    // @RequirePermission("kb:member:remove")
    @DeleteMapping("/{kbId}/members")
    public ApiResult<Boolean> removeMembers(@PathVariable String kbId, @RequestBody Collection<String> userIds) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        return ApiResult.success(kbMemberService.removeMembers(kbId, userIds));
    }

    // ========== 手动触发索引 ==========

    @Operation(summary = "手动触发知识库索引")
    @OperLog(title = "手动触发知识库索引", businessType = BusinessType.UPDATE)
    // @RequirePermission("kb:index")
    @PostMapping("/{kbId}/index")
    public ApiResult<Integer> index(@PathVariable String kbId, @RequestBody(required = false) Collection<String> fileTreeIds) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        int processed = knowledgeBaseIndexingService.indexFiles(kbId, fileTreeIds);
        return ApiResult.success(processed);
    }

    @Operation(summary = "获取知识库目录层级对应数据")
    @OperLog(title = "获取知识库目录层级对应数据", businessType = BusinessType.QUERY, isSaveResponseData = false)
    // @RequirePermission("kb:file:list")
    @GetMapping("/{kbId}/fs/list")
    public ApiResult<List<KbFileTree>> fsList(
            @PathVariable String kbId,
            @RequestParam(required = false) String parentId) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkVisible(kbId, userId);
        return ApiResult.success(kbFileTreeService.fsList(kbId, parentId));
    }

    @Operation(summary = "创建目录")
    @OperLog(title = "创建目录", businessType = BusinessType.INSERT)
    // @RequirePermission("kb:file:add")
    @PostMapping("/{kbId}/fs/folder")
    public ApiResult<Boolean> createFolder(
            @PathVariable String kbId,
            @RequestBody CreateFolderRequest request) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        return ApiResult.success(kbFileTreeService.createFolder(kbId, request.parentId(), request.name()));
    }

    @Operation(summary = "删除目录或文件节点")
    @OperLog(title = "删除目录或文件节点", businessType = BusinessType.DELETE)
    // @RequirePermission("kb:file:remove")
    @DeleteMapping("/{kbId}/fs/delete/{nodeId}")
    public ApiResult<Boolean> deleteNode(@PathVariable String kbId, @PathVariable String nodeId) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        return ApiResult.success(kbFileTreeService.deleteNode(nodeId));
    }

    @Operation(summary = "移动文件/目录")
    @OperLog(title = "移动文件/目录", businessType = BusinessType.UPDATE)
    // @RequirePermission("kb:file:update")
    @PutMapping("/{kbId}/tree/nodes/move")
    public ApiResult<Boolean> moveNodes(
            @PathVariable String kbId,
            @RequestBody MoveRequest request) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        return ApiResult.success(kbFileTreeService.move(kbId, request.ids(), request.targetParentId()));
    }

    @Operation(summary = "重命名文件/目录")
    @OperLog(title = "重命名文件/目录", businessType = BusinessType.UPDATE)
    // @RequirePermission("kb:file:update")
    @PutMapping("/{kbId}/tree/nodes/{nodeId}/rename")
    public ApiResult<Boolean> renameNode(
            @PathVariable String kbId,
            @PathVariable String nodeId,
            @RequestBody RenameRequest request) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);
        return ApiResult.success(kbFileTreeService.rename(nodeId, request.name()));
    }

    @Operation(summary = "批量上传文件到知识库")
    @OperLog(title = "批量上传文件到知识库", businessType = BusinessType.INSERT)
    // @RequirePermission("kb:file:add")
    @PostMapping("/{kbId}/files/upload")
    public ApiResult<List<String>> uploadFiles(
            @PathVariable String kbId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) String parentId
    ) {
        String userId = SecurityUtils.getUserId();
        knowledgeBaseService.checkOwner(kbId, userId);

        List<String> uploadedFileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                // forceNew=false，复用已有文件及其解析/分块/向量数据
                FileObject fileObj = fileManagerService.upload(file, null, "kb", kbId, false);
                if (fileObj != null) {
                    uploadedFileIds.add(fileObj.id());
                    // 创建目录树节点（默认添加到根目录）
                    kbFileTreeService.addFile(kbId, parentId, fileObj.id());
                }
            }
        }
        return ApiResult.success(uploadedFileIds);
    }

    // ========== 请求对象 ==========

    public record CreateFolderRequest(String parentId, String name) {}

    public record MoveRequest(List<String> ids, String targetParentId) {}

    public record RenameRequest(String name) {}
}
