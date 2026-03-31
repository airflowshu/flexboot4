package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaCascadePlatform;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.media.dto.PlaybackQueryRequest;
import com.yunlbd.flexboot4.media.dto.PlaybackRecordItem;
import com.yunlbd.flexboot4.media.dto.PlaybackStartRequest;
import com.yunlbd.flexboot4.media.dto.PtzControlRequest;

import java.util.List;

public interface MediaGatewayRuntimeManager {

    boolean reload(MediaGateway gateway, boolean autoStart);

    boolean start(MediaGateway gateway);

    boolean stop(String gatewayId);

    boolean isRunning(String gatewayId);

    MediaStreamSession startLive(MediaChannel channel);

    boolean stopLive(MediaStreamSession session);

    List<PlaybackRecordItem> queryPlayback(MediaChannel channel, PlaybackQueryRequest request);

    MediaStreamSession startPlayback(MediaChannel channel, PlaybackStartRequest request);

    boolean stopPlayback(MediaStreamSession session);

    boolean ptz(MediaChannel channel, PtzControlRequest request);

    boolean registerCascade(MediaCascadePlatform platform);

    boolean stopCascade(MediaCascadePlatform platform);
}
