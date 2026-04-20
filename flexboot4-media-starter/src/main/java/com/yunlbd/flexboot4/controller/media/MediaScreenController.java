package com.yunlbd.flexboot4.controller.media;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.media.MediaScreen;
import com.yunlbd.flexboot4.media.dto.MediaScreenDetail;
import com.yunlbd.flexboot4.media.dto.ScreenSaveRequest;
import com.yunlbd.flexboot4.service.media.MediaScreenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/media/screen")
@Tag(name = "分屏展示", description = "MediaScreen - 分屏展示")
@ApiTagGroup(group = "视频中心")
public class MediaScreenController extends BaseController<MediaScreenService, MediaScreen, String> {

    public MediaScreenController(MediaScreenService service) {
        super(service);
    }

    @Override
    public Class<MediaScreen> getEntityClass() {
        return MediaScreen.class;
    }

    @Operation(summary = "保存分屏方案")
    @RequirePermission("media:screen:save")
    @PostMapping("/save-layout")
    public ApiResult<MediaScreenDetail> saveLayout(@RequestBody ScreenSaveRequest request) {
        return ApiResult.success(service.saveScreen(request));
    }

    @Operation(summary = "查询分屏详情")
    @RequirePermission("media:screen:list")
    @GetMapping("/{id}/detail")
    public ApiResult<MediaScreenDetail> detail(@PathVariable String id) {
        return ApiResult.success(service.getDetail(id));
    }
}
