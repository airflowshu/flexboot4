package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseCrudController;
import com.yunlbd.flexboot4.controller.sys.CrudExcelSupport;
import com.yunlbd.flexboot4.controller.sys.CrudFieldPolicy;
import com.yunlbd.flexboot4.converter.ops.AiApiKeyCrudMapper;
import com.yunlbd.flexboot4.dto.ops.AiApiKeyCreateReq;
import com.yunlbd.flexboot4.dto.ops.AiApiKeyUpdateReq;
import com.yunlbd.flexboot4.entity.ops.AiApiKey;
import com.yunlbd.flexboot4.entity.sys.SysUser;
import com.yunlbd.flexboot4.excel.ops.AiApiKeyExportRow;
import com.yunlbd.flexboot4.excel.ops.AiApiKeyImportRow;
import com.yunlbd.flexboot4.service.ops.AiApiKeyService;
import com.yunlbd.flexboot4.util.AiApiKeyGenerator;
import com.yunlbd.flexboot4.vo.ops.AiApiKeyDetailVO;
import com.yunlbd.flexboot4.vo.ops.AiApiKeyListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/api-key")
@Tag(name = "apiKey管理", description = "AiApiKey - key管理")
@ApiTagGroup(group = "运维管理")
public class AiApiKeyController extends BaseCrudController<AiApiKeyService, AiApiKey, String,
        AiApiKeyCreateReq, AiApiKeyUpdateReq, AiApiKeyListVO, AiApiKeyDetailVO> {

    private final AiApiKeyCrudMapper mapper;

    public AiApiKeyController(AiApiKeyService service, AiApiKeyCrudMapper mapper) {
        super(service, mapper);
        this.mapper = mapper;
    }

    @Override
    public Class<AiApiKey> getEntityClass() {
        return AiApiKey.class;
    }


    @Override
    protected AiApiKey beforeCreate(AiApiKey entity, AiApiKeyCreateReq request) {
        //随机生成32位的字符的api_key
        entity.setApiKey(AiApiKeyGenerator.createKey());
        return entity;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.same(List.of(
                "id", "keyName", "userId", "status", "quote", "used", "modelScope",
                "expiresAt", "lastUsedTime", "notes", "remark", "createTime", "lastModifyTime"
        )).withQueryFields("user.username", "user.realName");
    }

    @Override
    protected CrudExcelSupport<AiApiKey, ?, ?> excelSupport() {
        return CrudExcelSupport.of(AiApiKeyExportRow.class, AiApiKeyImportRow.class, mapper::toExportRow, null);
    }

    @Operation(summary = "查询孤儿Key", description = "获取 user_id 不在 sys_user 表中的 API Key 列表")
    @RequirePermission("ai:api:key:list")
    @GetMapping("/orphaned-users")
    public ApiResult<List<SysUser>> getOrphanedUsers() {
        return ApiResult.success(service.selectOrphanedUsers());
    }


}
