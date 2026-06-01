package com.yunlbd.flexboot4.controller.media;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
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
@RequestMapping("/api/admin/media/zlm/hook/{serverId}")
@RequiredArgsConstructor
public class MediaZlmHookController {

    private final MediaHookValidator mediaHookValidator;
    private final MediaServerService mediaServerService;
    private final MediaStreamSessionService mediaStreamSessionService;
    private final ObjectMapper objectMapper;

    @RequirePermission(skip = true)
    @PostMapping("/on_stream_changed")
    public ApiResult<Boolean> onStreamChanged(@PathVariable("serverId") String serverId,
                                              @RequestBody String rawBody,
                                              @RequestHeader Map<String, String> headers,
                                              @RequestParam Map<String, String> params) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String app = stringValue(body.get("app"));
        String stream = stringValue(body.get("stream"));
        MediaStreamSession session = mediaStreamSessionService.findByStream(app, stream);
        MediaServer server = resolveServer(serverId);
        if (server == null) {
            return ApiResult.error("unknown media server");
        }
        if (!validateHook(server, rawBody, headers, body, params)) {
            return ApiResult.error("invalid hook signature");
        }
        if (server != null && server.getId() != null) {
            mediaServerService.markHookAlive(server.getId(), LocalDateTime.now());
        }

        boolean regist = boolValue(body.get("regist"));
        if (session != null) {
            if (regist) {
                String playUrl = session.getPlayUrl();
                String protocol = session.getPlayProtocol() == null || session.getPlayProtocol().isBlank()
                        ? "http-flv"
                        : session.getPlayProtocol();
                playUrl = mediaServerService.buildPlayUrls(server.getId(), app, stream).getOrDefault(protocol, playUrl);
                mediaStreamSessionService.markStreaming(session.getId(), playUrl);
            } else {
                mediaStreamSessionService.closeByStream(app, stream, LocalDateTime.now());
            }
        }
        return ApiResult.success(true);
    }

    @RequirePermission(skip = true)
    @PostMapping("/on_stream_none_reader")
    public ApiResult<Boolean> onStreamNoneReader(@PathVariable("serverId") String serverId,
                                                 @RequestBody String rawBody,
                                                 @RequestHeader Map<String, String> headers,
                                                 @RequestParam Map<String, String> params) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String app = stringValue(body.get("app"));
        String stream = stringValue(body.get("stream"));
        MediaServer server = resolveServer(serverId);
        if (server == null) {
            return ApiResult.error("unknown media server");
        }
        if (!validateHook(server, rawBody, headers, body, params)) {
            return ApiResult.error("invalid hook signature");
        }
        if (server != null && server.getId() != null) {
            mediaServerService.markHookAlive(server.getId(), LocalDateTime.now());
        }
        mediaStreamSessionService.closeByStream(app, stream, LocalDateTime.now());
        return ApiResult.success(true);
    }

    @RequirePermission(skip = true)
    @PostMapping("/on_server_keepalive")
    public ApiResult<Boolean> onServerKeepalive(@PathVariable("serverId") String serverId,
                                                @RequestBody String rawBody,
                                                @RequestHeader Map<String, String> headers,
                                                @RequestParam Map<String, String> params) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        MediaServer server = resolveServer(serverId);
        if (server == null) {
            return ApiResult.error("unknown media server");
        }
        if (!validateHook(server, rawBody, headers, body, params)) {
            return ApiResult.error("invalid hook signature");
        }
        mediaServerService.markHookAlive(server.getId(), LocalDateTime.now());
        return ApiResult.success(true);
    }

    @RequirePermission(skip = true)
    @PostMapping("/on_rtp_server_timeout")
    public ApiResult<Boolean> onRtpServerTimeout(@PathVariable("serverId") String serverId,
                                                 @RequestBody String rawBody,
                                                 @RequestHeader Map<String, String> headers,
                                                 @RequestParam Map<String, String> params) {
        Map<String, Object> body = parseBody(rawBody);
        if (body == null) {
            return ApiResult.error("invalid hook payload");
        }
        String app = stringValue(body.get("app"));
        String streamId = stringValue(body.get("stream_id"));
        if (streamId == null || streamId.isBlank()) {
            streamId = stringValue(body.get("stream"));
        }
        MediaServer server = resolveServer(serverId);
        if (server == null) {
            return ApiResult.error("unknown media server");
        }
        if (!validateHook(server, rawBody, headers, body, params)) {
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

    private MediaServer resolveServer(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return null;
        }
        return mediaServerService.getById(serverId);
    }

    private boolean validateHook(MediaServer server, String rawBody, Map<String, String> headers, Map<String, Object> body, Map<String, String> params) {
        if (server != null && server.getHookSecret() != null && !server.getHookSecret().isBlank()) {
            String secret = firstNonBlank(stringValue(body.get("secret")), params == null ? null : params.get("secret"));
            if (server.getHookSecret().equals(secret)) {
                return true;
            }
        }
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
