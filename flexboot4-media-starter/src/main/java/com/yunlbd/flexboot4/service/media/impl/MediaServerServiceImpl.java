package com.yunlbd.flexboot4.service.media.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.mapper.MediaServerMapper;
import com.yunlbd.flexboot4.media.MediaProperties;
import com.yunlbd.flexboot4.media.config.MediaRestClientFactory;
import com.yunlbd.flexboot4.media.core.MediaPlayUrlBuilder;
import com.yunlbd.flexboot4.media.core.ZlmClient;
import com.yunlbd.flexboot4.media.dto.MediaServerHookInfo;
import com.yunlbd.flexboot4.media.dto.MediaServerTestRequest;
import com.yunlbd.flexboot4.media.dto.MediaServerTestResult;
import com.yunlbd.flexboot4.media.enums.MediaServerStatus;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "mediaServer")
public class MediaServerServiceImpl extends BaseServiceImpl<MediaServerMapper, MediaServer> implements MediaServerService {

    private static final String HOOK_PATH_PREFIX = "/api/admin/media/zlm/hook";

    private final MediaProperties mediaProperties;
    private final MediaRestClientFactory mediaRestClientFactory;
    private final MediaPlayUrlBuilder mediaPlayUrlBuilder;

    @Override
    public MediaServerTestResult testConnection(MediaServerTestRequest request) {
        MediaServer server = resolveServer(request);
        if (server == null) {
            throw new IllegalArgumentException("Media server not found");
        }
        LocalDateTime now = LocalDateTime.now();
        try {
            ZlmClient client = createClient(server);
            Map<String, Object> versionResponse = client.version();
            List<Map<String, Object>> streams = client.getMediaList(null, null);
            server.setStatus(MediaServerStatus.HEALTHY);
            server.setLastTestTime(now);
            server.setLastError(null);
            if (server.getId() != null) {
                cacheProxy().updateById(server, true);
            }
            return new MediaServerTestResult(
                    true,
                    String.valueOf(versionResponse.getOrDefault("version", "unknown")),
                    streams.size(),
                    now,
                    "ZLMediaKit connection succeeded"
            );
        } catch (Exception e) {
            server.setStatus(MediaServerStatus.OFFLINE);
            server.setLastTestTime(now);
            server.setLastError(e.getMessage());
            if (server.getId() != null) {
                cacheProxy().updateById(server, true);
            }
            return new MediaServerTestResult(false, null, 0, now, e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> listStreams(String serverId, String app, String stream) {
        MediaServer server = serviceProxy(MediaServerService.class).getById(serverId);
        if (server == null) {
            throw new IllegalArgumentException("Media server not found");
        }
        return createClient(server).getMediaList(app, stream);
    }

    @Override
    public boolean closeStream(String serverId, String app, String stream, boolean force) {
        MediaServer server = cacheProxy().getById(serverId);
        if (server == null) {
            throw new IllegalArgumentException("Media server not found");
        }
        createClient(server).closeStreams(app, stream, force);
        return true;
    }

    @Override
    public Map<String, String> buildPlayUrls(String serverId, String app, String stream) {
        MediaServer server = cacheProxy().getById(serverId);
        if (server == null) {
            throw new IllegalArgumentException("Media server not found");
        }
        return mediaPlayUrlBuilder.build(server, app, stream);
    }

    @Override
    public MediaServerHookInfo buildHookInfo(String serverId) {
        MediaServer server = requireServer(serverId);
        String root = normalizeRoot(mediaProperties.callbackBaseUrl());
        String base = root + HOOK_PATH_PREFIX + "/" + server.getId();
        String adminParams = buildAdminParams(server);
        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("hook.on_stream_changed", base + "/on_stream_changed");
        urls.put("hook.on_stream_none_reader", base + "/on_stream_none_reader");
        urls.put("hook.on_server_keepalive", base + "/on_server_keepalive");
        urls.put("hook.on_rtp_server_timeout", base + "/on_rtp_server_timeout");
        return new MediaServerHookInfo(
                server.getId(),
                root,
                adminParams,
                urls.get("hook.on_stream_changed"),
                urls.get("hook.on_stream_none_reader"),
                urls.get("hook.on_server_keepalive"),
                urls.get("hook.on_rtp_server_timeout"),
                urls
        );
    }

    @Override
    public boolean syncHookConfig(String serverId) {
        MediaServer server = requireServer(serverId);
        if (server.getHookEnabled() != null && !server.getHookEnabled()) {
            throw new IllegalStateException("Media server hook is disabled");
        }
        MediaServerHookInfo hookInfo = buildHookInfo(serverId);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("hook.enable", 1);
        config.put("hook.admin_params", hookInfo.adminParams());
        config.putAll(hookInfo.urls());
        createClient(server).setServerConfig(config);
        return true;
    }

    @Override
    public void markHookAlive(String mediaServerId, LocalDateTime hookTime) {
        MediaServer server = cacheProxy().getById(mediaServerId);
        if (server == null) {
            return;
        }
        server.setLastHookTime(hookTime);
        server.setStatus(MediaServerStatus.HEALTHY);
        cacheProxy().updateById(server, true);
    }

    @Override
    public ZlmClient createClient(MediaServer server) {
        return new ZlmClient(mediaRestClientFactory.create(server.getBaseUrl()), server.getApiSecret());
    }

    private MediaServer resolveServer(MediaServerTestRequest request) {
        if (request.serverId() != null && !request.serverId().isBlank()) {
            MediaServer server = cacheProxy().getById(request.serverId());
            if (server != null) {
                return server;
            }
        }
        if (request.baseUrl() == null || request.baseUrl().isBlank()) {
            return null;
        }
        return MediaServer.builder()
                .baseUrl(request.baseUrl())
                .apiSecret(request.apiSecret())
                .serverName("temp-test")
                .serverType("ZLMEDIAKIT")
                .build();
    }

    private MediaServer requireServer(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("Media server id is required");
        }
        MediaServer server = cacheProxy().getById(serverId);
        if (server == null) {
            throw new IllegalArgumentException("Media server not found");
        }
        return server;
    }

    private String normalizeRoot(String value) {
        String root = value == null || value.isBlank() ? "http://localhost:8080" : value.trim();
        while (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        return root;
    }

    private String buildAdminParams(MediaServer server) {
        if (server.getHookSecret() != null && !server.getHookSecret().isBlank()) {
            return "secret=" + server.getHookSecret();
        }
        return "";
    }

    @Override
    protected java.util.Collection<String> extraInvalidateTables() {
        return List.of("media_gateway", "media_device", "media_channel", "media_stream_session");
    }
}
