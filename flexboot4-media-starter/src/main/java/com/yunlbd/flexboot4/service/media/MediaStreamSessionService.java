package com.yunlbd.flexboot4.service.media;

import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.time.LocalDateTime;
import java.util.List;

public interface MediaStreamSessionService extends IExtendedService<MediaStreamSession> {

    MediaStreamSession findActiveByChannel(String channelId, String sessionType);

    MediaStreamSession findByStream(String app, String stream);

    List<MediaStreamSession> listByDeviceId(String deviceId);

    void markStreaming(String sessionId, String playUrl);

    void closeSession(String sessionId, String status, LocalDateTime endTime);

    void closeByStream(String app, String stream, LocalDateTime endTime);
}
