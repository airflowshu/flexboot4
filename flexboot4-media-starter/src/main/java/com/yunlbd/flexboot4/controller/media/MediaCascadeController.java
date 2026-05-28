package com.yunlbd.flexboot4.controller.media;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.media.MediaCascadePlatformCreateReq;
import com.yunlbd.flexboot4.dto.media.MediaCascadePlatformUpdateReq;
import com.yunlbd.flexboot4.entity.media.MediaCascadePlatform;
import com.yunlbd.flexboot4.media.dto.CascadeBindRequest;
import com.yunlbd.flexboot4.service.media.MediaCascadePlatformService;
import com.yunlbd.flexboot4.vo.media.MediaCascadePlatformDetailVO;
import com.yunlbd.flexboot4.vo.media.MediaCascadePlatformListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/media/cascade")
@Tag(name = "国标级联", description = "MediaCascadePlatform - 国标级联")
@ApiTagGroup(group = "视频中心")
public class MediaCascadeController extends EntityCrudController<MediaCascadePlatformService, MediaCascadePlatform, String,
        MediaCascadePlatformCreateReq, MediaCascadePlatformUpdateReq, MediaCascadePlatformListVO, MediaCascadePlatformDetailVO> {

    public MediaCascadeController(MediaCascadePlatformService service) {
        super(service, MediaCascadePlatform.class, MediaCascadePlatformListVO.class, MediaCascadePlatformDetailVO.class);
    }

    @Override
    public Class<MediaCascadePlatform> getEntityClass() {
        return MediaCascadePlatform.class;
    }

    @Operation(summary = "保存级联绑定")
    @RequirePermission("media:cascade:bind")
    @PostMapping("/bind")
    public ApiResult<?> bind(@RequestBody CascadeBindRequest request) {
        return ApiResult.success(service.bindChannels(request));
    }

    @Operation(summary = "查询级联绑定")
    @RequirePermission("media:cascade:list")
    @GetMapping("/{id}/bindings")
    public ApiResult<?> bindings(@PathVariable("id") String id) {
        return ApiResult.success(service.listBindings(id));
    }

    @Operation(summary = "注册级联平台")
    @RequirePermission("media:cascade:bind")
    @PostMapping("/{id}/register")
    public ApiResult<Boolean> register(@PathVariable("id") String id) {
        return ApiResult.success(service.registerPlatform(id));
    }

    @Operation(summary = "停止级联平台")
    @RequirePermission("media:cascade:bind")
    @PostMapping("/{id}/stop")
    public ApiResult<Boolean> stop(@PathVariable("id") String id) {
        return ApiResult.success(service.stopPlatform(id));
    }
}
