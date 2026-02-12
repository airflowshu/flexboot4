package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.ops.AiApiKey;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.service.ops.AiApiKeyService;
import com.yunlbd.flexboot4.util.AiApiKeyGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-key")
@RequiredArgsConstructor
@Tag(name = "apiKey管理", description = "AiApiKey - key管理")
public class AiApiKeyController extends BaseController<AiApiKeyService, AiApiKey, String> {

    private final  AiApiKeyService aiApiKeyService;

    @Override
    public Class<AiApiKey> getEntityClass() {
        return AiApiKey.class;
    }


    @Override
    @Operation(summary = "Create", description = "Create entity.")
    @OperLog(businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResult<Boolean> create(@RequestBody AiApiKey entity) {
        //随机生成32位的字符的api_key
        entity.setApiKey(AiApiKeyGenerator.createKey());
        return ApiResult.success(service.save(entity));
    }

    @Operation(summary = "查询孤儿Key", description = "获取 user_id 不在 sys_user 表中的 API Key 列表")
    @GetMapping("/orphaned-users")
    public ApiResult<List<SysUser>> getOrphanedUsers() {
        return ApiResult.success(aiApiKeyService.selectOrphanedUsers());
    }


}