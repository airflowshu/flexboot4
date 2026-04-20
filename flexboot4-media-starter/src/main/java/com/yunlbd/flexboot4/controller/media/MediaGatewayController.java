package com.yunlbd.flexboot4.controller.media;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.media.dto.GatewayReloadRequest;
import com.yunlbd.flexboot4.service.media.MediaGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/media/gateway")
@Tag(name = "视频网关", description = "MediaGateway - 视频网关")
@ApiTagGroup(group = "视频中心")
public class MediaGatewayController extends BaseController<MediaGatewayService, MediaGateway, String> {

    public MediaGatewayController(MediaGatewayService service) {
        super(service);
    }

    @Override
    public Class<MediaGateway> getEntityClass() {
        return MediaGateway.class;
    }

    @Operation(summary = "重载视频网关")
    @RequirePermission("media:gateway:reload")
    @PostMapping("/reload")
    public ApiResult<Boolean> reload(@RequestBody GatewayReloadRequest request) {
        return ApiResult.success(service.reloadGateway(request));
    }

    @Operation(summary = "启动视频网关")
    @RequirePermission("media:gateway:reload")
    @PostMapping("/{id}/start")
    public ApiResult<Boolean> start(@PathVariable("id") String id) {
        return ApiResult.success(service.startGateway(id));
    }

    @Operation(summary = "停止视频网关")
    @RequirePermission("media:gateway:reload")
    @PostMapping("/{id}/stop")
    public ApiResult<Boolean> stop(@PathVariable("id") String id) {
        return ApiResult.success(service.stopGateway(id));
    }
}
