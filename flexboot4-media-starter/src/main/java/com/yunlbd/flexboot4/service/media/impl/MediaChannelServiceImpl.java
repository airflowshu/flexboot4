package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.mapper.MediaChannelMapper;
import com.yunlbd.flexboot4.media.MediaProperties;
import com.yunlbd.flexboot4.media.dto.ChannelLiveRequest;
import com.yunlbd.flexboot4.media.dto.MediaPlayResponse;
import com.yunlbd.flexboot4.media.dto.PlaybackQueryRequest;
import com.yunlbd.flexboot4.media.dto.PlaybackRecordItem;
import com.yunlbd.flexboot4.media.dto.PlaybackStartRequest;
import com.yunlbd.flexboot4.media.dto.PtzControlRequest;
import com.yunlbd.flexboot4.media.enums.MediaAccessType;
import com.yunlbd.flexboot4.media.enums.MediaPlayStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionType;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import com.yunlbd.flexboot4.service.media.MediaDeviceService;
import com.yunlbd.flexboot4.service.media.MediaGatewayRuntimeManager;
import com.yunlbd.flexboot4.service.media.MediaGatewayService;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@CacheConfig(cacheNames = "mediaChannel")
public class MediaChannelServiceImpl extends BaseServiceImpl<MediaChannelMapper, MediaChannel> implements MediaChannelService {

    private final MediaProperties mediaProperties;
    private final MediaDeviceService mediaDeviceService;
    private final MediaGatewayService mediaGatewayService;
    private final MediaGatewayRuntimeManager mediaGatewayRuntimeManager;
    private final MediaServerService mediaServerService;
    private final MediaStreamSessionService mediaStreamSessionService;

    public MediaChannelServiceImpl(
            MediaProperties mediaProperties,
            @Lazy MediaDeviceService mediaDeviceService,
            MediaGatewayService mediaGatewayService,
            @Lazy MediaGatewayRuntimeManager mediaGatewayRuntimeManager,
            MediaServerService mediaServerService,
            MediaStreamSessionService mediaStreamSessionService) {
        this.mediaProperties = mediaProperties;
        this.mediaDeviceService = mediaDeviceService;
        this.mediaGatewayService = mediaGatewayService;
        this.mediaGatewayRuntimeManager = mediaGatewayRuntimeManager;
        this.mediaServerService = mediaServerService;
        this.mediaStreamSessionService = mediaStreamSessionService;
    }

    @Override
    public List<MediaChannel> listByDeviceId(String deviceId) {
        return super.list(QueryWrapper.create()
                .from(MediaChannel.class)
                .where(MediaChannel::getDeviceId).eq(deviceId)
                .orderBy(MediaChannel::getCreateTime, true));
    }

    @Override
    public MediaChannel upsertChannel(MediaChannel channel) {
        MediaChannel existing = super.getOne(QueryWrapper.create()
                .from(MediaChannel.class)
                .where(MediaChannel::getChannelCode).eq(channel.getChannelCode()));
        if (existing == null) {
            save(channel);
            return channel;
        }
        channel.setId(existing.getId());
        updateById(channel, true);
        return super.getById(existing.getId());
    }

    @Override
    public void markChannelStatus(String channelCode, String status, LocalDateTime time) {
        MediaChannel channel = super.getOne(QueryWrapper.create()
                .from(MediaChannel.class)
                .where(MediaChannel::getChannelCode).eq(channelCode));
        if (channel == null) {
            return;
        }
        channel.setStatus(status);
        channel.setLastOfflineTime(time);
        updateById(channel, true);
    }

    @Override
    public MediaPlayResponse startLive(ChannelLiveRequest request) {
        MediaChannel channel = requireChannel(request.channelId());
        String protocol = resolveProtocol(request.protocol());
        MediaStreamSession existing = mediaStreamSessionService.findActiveByChannel(channel.getId(), MediaSessionType.LIVE);
        if (existing != null) {
            return buildResponse(existing, protocol);
        }

        MediaStreamSession session;
        if (isFixedAddress(channel)) {
            session = startFixedAddressLive(channel, protocol);
        } else {
            session = mediaGatewayRuntimeManager.startLive(channel);
            session.setPlayProtocol(protocol);
            mediaStreamSessionService.save(session);
        }

        channel.setPlayStatus(MediaPlayStatus.ONLINE);
        channel.setLastPlayTime(LocalDateTime.now());
        channel.setStreamApp(session.getStreamApp());
        channel.setStreamId(session.getStreamId());
        updateById(channel, true);
        return buildResponse(session, protocol);
    }

    @Override
    public boolean stopLive(String sessionId) {
        MediaStreamSession session = mediaStreamSessionService.getById(sessionId);
        if (session == null) {
            return true;
        }
        MediaChannel channel = super.getById(session.getChannelId());
        if (channel != null && isFixedAddress(channel)) {
            MediaServer server = resolveServer(channel);
            if (server != null) {
                if (session.getProxyKey() != null && !session.getProxyKey().isBlank()) {
                    mediaServerService.createClient(server).deleteStreamProxy(session.getProxyKey());
                } else {
                    mediaServerService.closeStream(server.getId(), session.getStreamApp(), session.getStreamId(), true);
                }
            }
        } else {
            mediaGatewayRuntimeManager.stopLive(session);
        }
        mediaStreamSessionService.closeSession(sessionId, MediaSessionStatus.CLOSED, LocalDateTime.now());
        if (channel != null) {
            channel.setPlayStatus(MediaPlayStatus.STOPPED);
            updateById(channel, true);
        }
        return true;
    }

    @Override
    public List<PlaybackRecordItem> queryPlayback(PlaybackQueryRequest request) {
        MediaChannel channel = requireChannel(request.channelId());
        if (isFixedAddress(channel)) {
            return List.of();
        }
        return mediaGatewayRuntimeManager.queryPlayback(channel, request);
    }

    @Override
    public MediaPlayResponse startPlayback(PlaybackStartRequest request) {
        MediaChannel channel = requireChannel(request.channelId());
        String protocol = resolveProtocol(request.protocol());
        MediaStreamSession session = mediaGatewayRuntimeManager.startPlayback(channel, request);
        session.setPlayProtocol(protocol);
        mediaStreamSessionService.save(session);
        channel.setPlayStatus(MediaPlayStatus.ONLINE);
        channel.setLastPlayTime(LocalDateTime.now());
        channel.setStreamApp(session.getStreamApp());
        channel.setStreamId(session.getStreamId());
        updateById(channel, true);
        return buildResponse(session, protocol);
    }

    @Override
    public boolean stopPlayback(String sessionId) {
        MediaStreamSession session = mediaStreamSessionService.getById(sessionId);
        if (session == null) {
            return true;
        }
        mediaGatewayRuntimeManager.stopPlayback(session);
        mediaStreamSessionService.closeSession(sessionId, MediaSessionStatus.CLOSED, LocalDateTime.now());
        MediaChannel channel = super.getById(session.getChannelId());
        if (channel != null) {
            channel.setPlayStatus(MediaPlayStatus.STOPPED);
            channel.setLastOfflineTime(LocalDateTime.now());
            updateById(channel, true);
        }
        return true;
    }

    @Override
    public boolean ptzControl(PtzControlRequest request) {
        MediaChannel channel = requireChannel(request.channelId());
        if (isFixedAddress(channel)) {
            return false;
        }
        return mediaGatewayRuntimeManager.ptz(channel, request);
    }

    private MediaStreamSession startFixedAddressLive(MediaChannel channel, String protocol) {
        if (channel.getFixedUrl() == null || channel.getFixedUrl().isBlank()) {
            throw new IllegalArgumentException("Fixed address channel does not have a media URL");
        }
        MediaServer server = resolveServer(channel);
        if (server == null) {
            throw new IllegalArgumentException("No media server available for fixed address streaming");
        }
        String app = "proxy";
        String stream = sanitizeStreamId(channel.getStreamId() == null || channel.getStreamId().isBlank() ? channel.getChannelCode() : channel.getStreamId());
        Map<String, Object> response = mediaServerService.createClient(server).addStreamProxy(app, stream, channel.getFixedUrl());
        MediaStreamSession session = MediaStreamSession.builder()
                .serverId(server.getId())
                .gatewayId(channel.getGatewayId())
                .deviceId(channel.getDeviceId())
                .channelId(channel.getId())
                .sessionType(MediaSessionType.LIVE)
                .streamApp(app)
                .streamId(stream)
                .playProtocol(protocol)
                .proxyKey(extractProxyKey(response))
                .status(MediaSessionStatus.STREAMING)
                .startedTime(LocalDateTime.now())
                .build();
        mediaStreamSessionService.save(session);
        mediaStreamSessionService.markStreaming(session.getId(), mediaServerService.buildPlayUrls(server.getId(), app, stream).get(protocol));
        return mediaStreamSessionService.getById(session.getId());
    }

    private MediaPlayResponse buildResponse(MediaStreamSession session, String protocol) {
        Map<String, String> urls = session.getServerId() == null
                ? Map.of()
                : mediaServerService.buildPlayUrls(session.getServerId(), session.getStreamApp(), session.getStreamId());
        String selectedProtocol = urls.containsKey(protocol) ? protocol : mediaProperties.defaultPlayProtocol();
        return new MediaPlayResponse(
                session.getId(),
                session.getStatus(),
                session.getStreamApp(),
                session.getStreamId(),
                selectedProtocol,
                urls
        );
    }

    private MediaChannel requireChannel(String channelId) {
        MediaChannel channel = super.getById(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not found");
        }
        return channel;
    }

    private boolean isFixedAddress(MediaChannel channel) {
        MediaDevice device = mediaDeviceService.getById(channel.getDeviceId());
        return device != null && MediaAccessType.FIXED_ADDRESS.equalsIgnoreCase(device.getAccessType());
    }

    private String resolveProtocol(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested;
        }
        return mediaProperties.defaultPlayProtocol();
    }

    private MediaServer resolveServer(MediaChannel channel) {
        if (channel.getServerId() != null && !channel.getServerId().isBlank()) {
            return mediaServerService.getById(channel.getServerId());
        }
        MediaDevice device = mediaDeviceService.getById(channel.getDeviceId());
        if (device != null && device.getServerId() != null && !device.getServerId().isBlank()) {
            return mediaServerService.getById(device.getServerId());
        }
        MediaGateway gateway = channel.getGatewayId() == null ? mediaGatewayService.getActiveGateway() : mediaGatewayService.getById(channel.getGatewayId());
        if (gateway != null && gateway.getServerId() != null && !gateway.getServerId().isBlank()) {
            return mediaServerService.getById(gateway.getServerId());
        }
        return mediaServerService.getOne(QueryWrapper.create()
                .from(MediaServer.class)
                .where(MediaServer::getEnabled).eq(true)
                .orderBy(MediaServer::getCreateTime, false));
    }

    private String sanitizeStreamId(String raw) {
        String sanitized = raw == null || raw.isBlank() ? UUID.randomUUID().toString().replace("-", "") : raw;
        return sanitized.replaceAll("[^0-9A-Za-z_-]", "_");
    }

    private String extractProxyKey(Map<String, Object> response) {
        Object key = response.get("key");
        if (key != null) {
            return String.valueOf(key);
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map && map.get("key") != null) {
            return String.valueOf(map.get("key"));
        }
        return null;
    }
}
