package com.yunlbd.flexboot4.task.media;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaChannel;
import com.yunlbd.flexboot4.entity.media.MediaDevice;
import com.yunlbd.flexboot4.entity.media.MediaGateway;
import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.media.MediaProperties;
import com.yunlbd.flexboot4.media.enums.MediaAccessType;
import com.yunlbd.flexboot4.media.enums.MediaGatewayRuntimeStatus;
import com.yunlbd.flexboot4.media.enums.MediaOnlineStatus;
import com.yunlbd.flexboot4.media.enums.MediaPlayStatus;
import com.yunlbd.flexboot4.media.enums.MediaServerStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionStatus;
import com.yunlbd.flexboot4.media.enums.MediaSessionType;
import com.yunlbd.flexboot4.service.media.MediaChannelService;
import com.yunlbd.flexboot4.service.media.MediaDeviceService;
import com.yunlbd.flexboot4.service.media.MediaGatewayRuntimeManager;
import com.yunlbd.flexboot4.service.media.MediaGatewayService;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaRuntimeMaintenanceTask {

    private static final String DEVICE_TIMEOUT_STATUS = "TIMEOUT";

    private final MediaProperties mediaProperties;
    private final MediaServerService mediaServerService;
    private final MediaGatewayService mediaGatewayService;
    private final MediaGatewayRuntimeManager mediaGatewayRuntimeManager;
    private final MediaDeviceService mediaDeviceService;
    private final MediaChannelService mediaChannelService;
    private final MediaStreamSessionService mediaStreamSessionService;

    @Scheduled(
            initialDelayString = "${media.runtime-check-initial-delay-millis:30000}",
            fixedDelayString = "${media.runtime-check-fixed-delay-millis:30000}"
    )
    public void reconcileRuntime() {
        if (!mediaProperties.enabled() || !mediaProperties.runtimeCheckEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            recoverGatewayRuntime();
            markServerHookTimeout(now);
            markDeviceKeepaliveTimeout(now);
            cleanupSessionTimeout(now);
        } catch (Exception e) {
            log.warn("Media runtime maintenance failed", e);
        }
    }

    private void recoverGatewayRuntime() {
        List<MediaGateway> gateways = mediaGatewayService.list(QueryWrapper.create()
                .from(MediaGateway.class)
                .where(MediaGateway::getEnabled).eq(true));
        for (MediaGateway gateway : gateways) {
            if (gateway.getId() == null) {
                continue;
            }
            boolean running = mediaGatewayRuntimeManager.isRunning(gateway.getId());
            if (running) {
                if (!MediaGatewayRuntimeStatus.RUNNING.equals(gateway.getRuntimeStatus())) {
                    gateway.setRuntimeStatus(MediaGatewayRuntimeStatus.RUNNING);
                    gateway.setLastError(null);
                    mediaGatewayService.updateById(gateway, true);
                }
                continue;
            }
            if (!MediaGatewayRuntimeStatus.RUNNING.equals(gateway.getRuntimeStatus())) {
                continue;
            }
            if (mediaProperties.gatewayAutoRecover()) {
                boolean recovered = mediaGatewayService.startGateway(gateway.getId());
                if (!recovered) {
                    log.warn("Gateway runtime recovery failed, gatewayId={}", gateway.getId());
                }
                continue;
            }
            gateway.setRuntimeStatus(MediaGatewayRuntimeStatus.ERROR);
            gateway.setLastError("Gateway runtime lost, reload may be required");
            mediaGatewayService.updateById(gateway, true);
        }
    }

    private void markServerHookTimeout(LocalDateTime now) {
        long timeout = mediaProperties.serverHookTimeoutSeconds();
        if (timeout <= 0) {
            return;
        }
        List<MediaServer> servers = mediaServerService.list(QueryWrapper.create()
                .from(MediaServer.class)
                .where(MediaServer::getEnabled).eq(true));
        for (MediaServer server : servers) {
            LocalDateTime heartbeat = firstNonNull(server.getLastHookTime(), server.getLastTestTime(), server.getCreateTime());
            if (heartbeat == null || !heartbeat.plusSeconds(timeout).isBefore(now)) {
                continue;
            }
            if (MediaServerStatus.OFFLINE.equals(server.getStatus())) {
                continue;
            }
            server.setStatus(MediaServerStatus.OFFLINE);
            server.setLastError("ZLM hook timeout");
            mediaServerService.updateById(server, true);
        }
    }

    private void markDeviceKeepaliveTimeout(LocalDateTime now) {
        long timeout = mediaProperties.deviceKeepaliveTimeoutSeconds();
        if (timeout <= 0) {
            return;
        }
        List<MediaDevice> devices = mediaDeviceService.list(QueryWrapper.create()
                .from(MediaDevice.class)
                .where(MediaDevice::getAccessType).eq(MediaAccessType.GB28181)
                .and(MediaDevice::getOnlineStatus).eq(MediaOnlineStatus.ONLINE));
        for (MediaDevice device : devices) {
            LocalDateTime heartbeat = firstNonNull(device.getLastKeepaliveTime(), device.getLastRegisterTime(), device.getLastModifyTime(), device.getCreateTime());
            if (heartbeat == null || !heartbeat.plusSeconds(timeout).isBefore(now)) {
                continue;
            }
            device.setOnlineStatus(MediaOnlineStatus.OFFLINE);
            device.setRegisterStatus(DEVICE_TIMEOUT_STATUS);
            mediaDeviceService.updateById(device, true);
            markDeviceChannelsOffline(device.getId(), now);
        }
    }

    private void markDeviceChannelsOffline(String deviceId, LocalDateTime now) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        List<MediaChannel> channels = mediaChannelService.list(QueryWrapper.create()
                .from(MediaChannel.class)
                .where(MediaChannel::getDeviceId).eq(deviceId));
        for (MediaChannel channel : channels) {
            channel.setStatus(MediaOnlineStatus.OFFLINE);
            channel.setPlayStatus(MediaPlayStatus.STOPPED);
            channel.setLastOfflineTime(now);
            mediaChannelService.updateById(channel, true);
        }
    }

    private void cleanupSessionTimeout(LocalDateTime now) {
        List<MediaStreamSession> sessions = mediaStreamSessionService.list(QueryWrapper.create()
                .from(MediaStreamSession.class)
                .where(MediaStreamSession::getStatus).in(MediaSessionStatus.PENDING, MediaSessionStatus.STREAMING));
        for (MediaStreamSession session : sessions) {
            LocalDateTime reference = firstNonNull(session.getLastModifyTime(), session.getStartedTime(), session.getCreateTime());
            if (reference == null) {
                continue;
            }
            String targetStatus = null;
            if (MediaSessionStatus.PENDING.equals(session.getStatus())
                    && mediaProperties.pendingSessionTimeoutSeconds() > 0
                    && reference.plusSeconds(mediaProperties.pendingSessionTimeoutSeconds()).isBefore(now)) {
                targetStatus = MediaSessionStatus.FAILED;
            }
            if (MediaSessionStatus.STREAMING.equals(session.getStatus())
                    && mediaProperties.streamingSessionTimeoutSeconds() > 0
                    && reference.plusSeconds(mediaProperties.streamingSessionTimeoutSeconds()).isBefore(now)) {
                targetStatus = MediaSessionStatus.CLOSED;
            }
            if (targetStatus == null) {
                continue;
            }
            forceReleaseSession(session, now, targetStatus);
        }
    }

    private void forceReleaseSession(MediaStreamSession session, LocalDateTime now, String targetStatus) {
        try {
            if (MediaSessionType.PLAYBACK.equals(session.getSessionType())) {
                mediaGatewayRuntimeManager.stopPlayback(session);
            } else {
                mediaGatewayRuntimeManager.stopLive(session);
            }
        } catch (Exception e) {
            log.debug("Ignore gateway release failure for session {}", session.getId(), e);
        }
        try {
            if (session.getServerId() != null
                    && session.getStreamApp() != null && !session.getStreamApp().isBlank()
                    && session.getStreamId() != null && !session.getStreamId().isBlank()) {
                mediaServerService.closeStream(session.getServerId(), session.getStreamApp(), session.getStreamId(), true);
            }
        } catch (Exception e) {
            log.debug("Ignore server stream close failure for session {}", session.getId(), e);
        }
        mediaStreamSessionService.closeSession(session.getId(), targetStatus, now);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

