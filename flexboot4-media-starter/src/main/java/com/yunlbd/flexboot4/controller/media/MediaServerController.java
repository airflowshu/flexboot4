package com.yunlbd.flexboot4.controller.media;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.media.MediaServerCreateReq;
import com.yunlbd.flexboot4.dto.media.MediaServerUpdateReq;
import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.media.dto.MediaServerTestRequest;
import com.yunlbd.flexboot4.media.dto.MediaServerTestResult;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.vo.media.MediaServerDetailVO;
import com.yunlbd.flexboot4.vo.media.MediaServerListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/media/server")
@Tag(name = "流媒体服务", description = "MediaServer - 流媒体服务")
@ApiTagGroup(group = "视频中心")
public class MediaServerController extends EntityCrudController<MediaServerService, MediaServer, String,
        MediaServerCreateReq, MediaServerUpdateReq, MediaServerListVO, MediaServerDetailVO> {

    public MediaServerController(MediaServerService service) {
        super(service, MediaServer.class, MediaServerListVO.class, MediaServerDetailVO.class);
    }

    @Override
    public Class<MediaServer> getEntityClass() {
        return MediaServer.class;
    }

    @Operation(summary = "测试流媒体服务")
    @RequirePermission("media:server:test")
    @PostMapping("/test")
    public ApiResult<MediaServerTestResult> test(@RequestBody MediaServerTestRequest request) {
        return ApiResult.success(service.testConnection(request));
    }

    @Operation(summary = "查询流列表")
    @RequirePermission("media:server:list")
    @GetMapping("/{id}/streams")
    public ApiResult<List<Map<String, Object>>> streams(@PathVariable String id,
                                                        @RequestParam(value = "app", required = false) String app,
                                                        @RequestParam(value = "stream", required = false) String stream) {
        return ApiResult.success(service.listStreams(id, app, stream));
    }

    @Operation(summary = "关闭流")
    @RequirePermission("media:server:test")
    @PostMapping("/{id}/close-stream")
    public ApiResult<Boolean> closeStream(@PathVariable String id,
                                          @RequestParam("app") String app,
                                          @RequestParam("stream") String stream,
                                          @RequestParam(value = "force", defaultValue = "true") boolean force) {
        return ApiResult.success(service.closeStream(id, app, stream, force));
    }
}
