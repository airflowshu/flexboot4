package com.yunlbd.flexboot4.controller.media;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.media.dto.*;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/media/channel")
@RequiredArgsConstructor
@Tag(name = "视频通道", description = "MediaChannel - 视频通道")
@ApiTagGroup(group = "视频中心")
public class MediaChannelController extends BaseController<MediaChannelService, MediaChannel, String> {

    private final MediaChannelService mediaChannelService;
    @Override
    public Class<MediaChannel> getEntityClass() {
        return MediaChannel.class;
    }

    @Operation(summary = "实时播放")
    @RequirePermission("media:channel:live")
    @PostMapping("/live")
    public ApiResult<MediaPlayResponse> live(@RequestBody ChannelLiveRequest request) {
        return ApiResult.success(mediaChannelService.startLive(request));
    }

    @Operation(summary = "停止实时播放")
    @RequirePermission("media:channel:live")
    @PostMapping("/live/stop/{sessionId}")
    public ApiResult<Boolean> stopLive(@PathVariable("sessionId") String sessionId) {
        return ApiResult.success(mediaChannelService.stopLive(sessionId));
    }

    @Operation(summary = "录像查询")
    @RequirePermission("media:channel:playback")
    @PostMapping("/playback/query")
    public ApiResult<List<PlaybackRecordItem>> playbackQuery(@RequestBody PlaybackQueryRequest request) {
        return ApiResult.success(mediaChannelService.queryPlayback(request));
    }

    @Operation(summary = "开始录像回放")
    @RequirePermission("media:channel:playback")
    @PostMapping("/playback/start")
    public ApiResult<MediaPlayResponse> playbackStart(@RequestBody PlaybackStartRequest request) {
        return ApiResult.success(mediaChannelService.startPlayback(request));
    }

    @Operation(summary = "停止录像回放")
    @RequirePermission("media:channel:playback")
    @PostMapping("/playback/stop/{sessionId}")
    public ApiResult<Boolean> playbackStop(@PathVariable("sessionId") String sessionId) {
        return ApiResult.success(mediaChannelService.stopPlayback(sessionId));
    }

    @Operation(summary = "云台控制")
    @RequirePermission("media:channel:ptz")
    @PostMapping("/ptz")
    public ApiResult<Boolean> ptz(@RequestBody PtzControlRequest request) {
        return ApiResult.success(mediaChannelService.ptzControl(request));
    }
}
