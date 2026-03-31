package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.mapper.MediaChannelMapper;
import com.yunlbd.flexboot4.mapper.MediaStreamSessionMapper;
import com.yunlbd.flexboot4.media.enums.MediaPlayStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionStatus;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaStreamSession")
public class MediaStreamSessionServiceImpl extends BaseServiceImpl<MediaStreamSessionMapper, MediaStreamSession> implements MediaStreamSessionService {

    private final MediaChannelMapper mediaChannelMapper;

    @Override
    public MediaStreamSession findActiveByChannel(String channelId, String sessionType) {
        return getOne(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getChannelId).eq(channelId)
                .and(MediaStreamSession::getSessionType).eq(sessionType)
                .and(MediaStreamSession::getStatus).ne(MediaSessionStatus.CLOSED)
                .orderBy(MediaStreamSession::getCreateTime, false));
    }

    @Override
    public MediaStreamSession findByStream(String app, String stream) {
        return getOne(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getStreamApp).eq(app)
                .and(MediaStreamSession::getStreamId).eq(stream)
                .and(MediaStreamSession::getStatus).ne(MediaSessionStatus.CLOSED)
                .orderBy(MediaStreamSession::getCreateTime, false));
    }

    @Override
    public List<MediaStreamSession> listByDeviceId(String deviceId) {
        return list(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getDeviceId).eq(deviceId)
                .orderBy(MediaStreamSession::getCreateTime, false));
    }

    @Override
    public void markStreaming(String sessionId, String playUrl) {
        MediaStreamSession session = getById(sessionId);
        if (session == null) {
            return;
        }
        session.setPlayUrl(playUrl);
        session.setStatus(MediaSessionStatus.STREAMING);
        updateById(session, true);
        markChannelPlaying(session.getChannelId());
    }

    @Override
    public void closeSession(String sessionId, String status, LocalDateTime endTime) {
        MediaStreamSession session = getById(sessionId);
        if (session == null) {
            return;
        }
        session.setStatus(status);
        session.setEndedTime(endTime);
        updateById(session, true);
        markChannelStoppedIfNoActiveSession(session.getChannelId(), endTime);
    }

    @Override
    public void closeByStream(String app, String stream, LocalDateTime endTime) {
        List<MediaStreamSession> sessions = list(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getStreamApp).eq(app)
                .and(MediaStreamSession::getStreamId).eq(stream)
                .and(MediaStreamSession::getStatus).ne(MediaSessionStatus.CLOSED));
        for (MediaStreamSession session : sessions) {
            session.setStatus(MediaSessionStatus.CLOSED);
            session.setEndedTime(endTime);
            updateById(session, true);
            markChannelStoppedIfNoActiveSession(session.getChannelId(), endTime);
        }
    }

    private void markChannelPlaying(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        MediaChannel channel = mediaChannelMapper.selectOneById(channelId);
        if (channel == null) {
            return;
        }
        channel.setPlayStatus(MediaPlayStatus.ONLINE);
        updateChannel(channel);
    }

    private void markChannelStoppedIfNoActiveSession(String channelId, LocalDateTime time) {
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        boolean hasActive = !list(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getChannelId).eq(channelId)
                .and(MediaStreamSession::getStatus).in(MediaSessionStatus.PENDING, MediaSessionStatus.STREAMING))
                .isEmpty();
        if (hasActive) {
            return;
        }
        MediaChannel channel = mediaChannelMapper.selectOneById(channelId);
        if (channel == null) {
            return;
        }
        channel.setPlayStatus(MediaPlayStatus.STOPPED);
        channel.setLastOfflineTime(time);
        updateChannel(channel);
    }

    private void updateChannel(MediaChannel channel) {
        if (channel.getId() == null) {
            return;
        }
        mediaChannelMapper.update(channel, true);
    }
}
