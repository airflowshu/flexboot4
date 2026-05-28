package com.yunlbd.flexboot4.controller.media;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.media.MediaDeviceCreateReq;
import com.yunlbd.flexboot4.dto.media.MediaDeviceUpdateReq;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.media.dto.MediaDeviceDetail;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import com.yunlbd.flexboot4.service.media.MediaDeviceService;
import com.yunlbd.flexboot4.vo.media.MediaDeviceDetailVO;
import com.yunlbd.flexboot4.vo.media.MediaDeviceListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/media/device")
@Tag(name = "视频设备", description = "MediaDevice - 视频设备")
@ApiTagGroup(group = "视频中心")
public class MediaDeviceController extends EntityCrudController<MediaDeviceService, MediaDevice, String,
        MediaDeviceCreateReq, MediaDeviceUpdateReq, MediaDeviceListVO, MediaDeviceDetailVO> {

    public MediaDeviceController(MediaDeviceService service, MediaChannelService mediaChannelService) {
        super(service, MediaDevice.class, MediaDeviceListVO.class, MediaDeviceDetailVO.class);
        this.mediaChannelService = mediaChannelService;
    }
    private final MediaChannelService mediaChannelService;

    @Override
    public Class<MediaDevice> getEntityClass() {
        return MediaDevice.class;
    }

    @Operation(summary = "查询设备详情")
    @RequirePermission("media:device:list")
    @GetMapping("/{id}/detail")
    public ApiResult<MediaDeviceDetail> detail(@PathVariable("id") String id) {
        return ApiResult.success(service.getDetail(id));
    }

    @Operation(summary = "查询设备通道")
    @RequirePermission("media:device:list")
    @GetMapping("/{id}/channels")
    public ApiResult<?> channels(@PathVariable("id") String id) {
        return ApiResult.success(mediaChannelService.listByDeviceId(id));
    }
}
