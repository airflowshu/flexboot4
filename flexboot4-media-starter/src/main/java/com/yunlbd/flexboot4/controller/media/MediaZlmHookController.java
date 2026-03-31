package com.yunlbd.flexboot4.controller.media;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.entity.media.MediaStreamSession;
import com.yunlbd.flexboot4.media.core.MediaHookValidator;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.service.media.MediaStreamSessionService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Hidden
@RestController
@RequestMapping("/api/admin/media/zlm/hook")
@RequiredArgsConstructor
public class MediaZlmHookController {

    private final MediaHookValidator mediaHookValidator;
    private final MediaServerService mediaServerService;
    private final MediaStreamSessionService mediaStreamSessionService;
    private final ObjectMapper objectMapper;

    @PostMapping("/on_stream_changed")
    public ApiResult<Boolean> onStreamChanged(@RequestBody String rawBody, @RequestHeader Map<String, String> headers) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String app = stringValue(body.get("app"));
        String stream = stringValue(body.get("stream"));
        MediaStreamSession session = mediaStreamSessionService.findByStream(app, stream);
        MediaServer server = resolveServer(body, session);
        if (!validateHook(server, rawBody, headers)) {
            return ApiResult.error("invalid hook signature");
        }
        if (server != null && server.getId() != null) {
            mediaServerService.markHookAlive(server.getId(), LocalDateTime.now());
        }

        String mediaServerId = stringValue(body.get("mediaServerId"));
        if (server == null && mediaServerId != null) {
            mediaServerService.markHookAlive(mediaServerId, LocalDateTime.now());
        }

        boolean regist = boolValue(body.get("regist"));
        if (session != null) {
            if (regist) {
                if (server == null && session.getServerId() != null) {
                    server = mediaServerService.getById(session.getServerId());
                }
                String playUrl = session.getPlayUrl();
                if (server != null) {
                    String protocol = session.getPlayProtocol() == null || session.getPlayProtocol().isBlank()
                            ? "http-flv"
                            : session.getPlayProtocol();
                    playUrl = mediaServerService.buildPlayUrls(server.getId(), app, stream).getOrDefault(protocol, playUrl);
                }
                mediaStreamSessionService.markStreaming(session.getId(), playUrl);
            } else {
                mediaStreamSessionService.closeByStream(app, stream, LocalDateTime.now());
            }
        }
        return ApiResult.success(true);
    }

    @PostMapping("/on_stream_none_reader")
    public ApiResult<Boolean> onStreamNoneReader(@RequestBody String rawBody, @RequestHeader Map<String, String> headers) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String app = stringValue(body.get("app"));
        String stream = stringValue(body.get("stream"));
        MediaStreamSession session = mediaStreamSessionService.findByStream(app, stream);
        MediaServer server = resolveServer(body, session);
        if (!validateHook(server, rawBody, headers)) {
            return ApiResult.error("invalid hook signature");
        }
        if (server != null && server.getId() != null) {
            mediaServerService.markHookAlive(server.getId(), LocalDateTime.now());
        }
        mediaStreamSessionService.closeByStream(app, stream, LocalDateTime.now());
        return ApiResult.success(true);
    }

    @PostMapping("/on_server_keepalive")
    public ApiResult<Boolean> onServerKeepalive(@RequestBody String rawBody, @RequestHeader Map<String, String> headers) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String mediaServerId = stringValue(body.get("mediaServerId"));
        MediaServer server = mediaServerId == null ? null : mediaServerService.getById(mediaServerId);
        if (!validateHook(server, rawBody, headers)) {
            return ApiResult.error("invalid hook signature");
        }
        if (mediaServerId != null) {
            mediaServerService.markHookAlive(mediaServerId, LocalDateTime.now());
        }
        return ApiResult.success(true);
    }

    @PostMapping("/on_rtp_server_timeout")
    public ApiResult<Boolean> onRtpServerTimeout(@RequestBody String rawBody, @RequestHeader Map<String, String> headers) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String app = stringValue(body.get("app"));
        String streamId = stringValue(body.get("stream_id"));
        if (streamId == null || streamId.isBlank()) {
            streamId = stringValue(body.get("stream"));
        }
        MediaStreamSession session = mediaStreamSessionService.findByStream(app == null ? "rtp" : app, streamId);
        MediaServer server = resolveServer(body, session);
        if (!validateHook(server, rawBody, headers)) {
            return ApiResult.error("invalid hook signature");
        }
        if (server != null && server.getId() != null) {
            mediaServerService.markHookAlive(server.getId(), LocalDateTime.now());
        }
        if (streamId != null) {
            mediaStreamSessionService.closeByStream(app == null || app.isBlank() ? "rtp" : app, streamId, LocalDateTime.now());
        }
        return ApiResult.success(true);
    }

    private Map<String, Object> parseBody(String rawBody) {
        try {
            if (rawBody == null || rawBody.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(rawBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }

    private MediaServer resolveServer(Map<String, Object> body, MediaStreamSession session) {
        String mediaServerId = stringValue(body.get("mediaServerId"));
        if (mediaServerId != null && !mediaServerId.isBlank()) {
            MediaServer server = mediaServerService.getById(mediaServerId);
            if (server != null) {
                return server;
            }
        }
        if (session != null && session.getServerId() != null && !session.getServerId().isBlank()) {
            return mediaServerService.getById(session.getServerId());
        }
        return null;
    }

    private boolean validateHook(MediaServer server, String rawBody, Map<String, String> headers) {
        String signature = headerValue(headers, mediaHookValidator.signatureHeaderName(), "X-Media-Hook-Signature");
        String timestamp = headerValue(headers, mediaHookValidator.timestampHeaderName(), "X-Media-Hook-Timestamp");
        return mediaHookValidator.validate(server, rawBody == null ? "" : rawBody, signature, timestamp);
    }

    private String headerValue(Map<String, String> headers, String... names) {
        if (headers == null || headers.isEmpty() || names == null) {
            return null;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private boolean boolValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return false;
        }
        return "1".equals(text)
                || "true".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text)
                || "on".equalsIgnoreCase(text);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
