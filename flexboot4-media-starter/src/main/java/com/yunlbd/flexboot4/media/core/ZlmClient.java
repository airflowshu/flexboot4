package com.yunlbd.flexboot4.media.core;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZlmClient {

    private final RestClient restClient;
    private final String secret;

    public ZlmClient(RestClient restClient, String secret) {
        this.restClient = restClient;
        this.secret = secret;
    }

    public Map<String, Object> version() {
        return post("/index/api/version", Map.of());
    }

    public List<Map<String, Object>> getServerConfig() {
        return extractList(post("/index/api/getServerConfig", Map.of()));
    }

    public List<Map<String, Object>> getMediaList(String app, String stream) {
        Map<String, Object> params = new HashMap<>();
        if (app != null && !app.isBlank()) {
            params.put("app", app);
        }
        if (stream != null && !stream.isBlank()) {
            params.put("stream", stream);
        }
        return extractList(post("/index/api/getMediaList", params));
    }

    public Map<String, Object> addStreamProxy(String app, String stream, String url) {
        Map<String, Object> params = new HashMap<>();
        params.put("vhost", "__defaultVhost__");
        params.put("app", app);
        params.put("stream", stream);
        params.put("url", url);
        params.put("enable_mp4", 0);
        params.put("enable_hls", 1);
        return post("/index/api/addStreamProxy", params);
    }

    public Map<String, Object> deleteStreamProxy(String key) {
        return post("/index/api/delStreamProxy", Map.of("key", key));
    }

    public Map<String, Object> closeStreams(String app, String stream, boolean force) {
        Map<String, Object> params = new HashMap<>();
        params.put("vhost", "__defaultVhost__");
        params.put("schema", "rtmp");
        params.put("app", app);
        params.put("stream", stream);
        params.put("force", force ? 1 : 0);
        return post("/index/api/close_streams", params);
    }

    public int openRtpServer(String streamId, int port, int tcpMode) {
        Map<String, Object> response = post("/index/api/openRtpServer", Map.of(
                "stream_id", streamId,
                "port", port,
                "tcp_mode", tcpMode
        ));
        Object value = response.get("port");
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException("ZLM did not return RTP port");
    }

    public Map<String, Object> closeRtpServer(String streamId) {
        return post("/index/api/closeRtpServer", Map.of("stream_id", streamId));
    }

    public Map<String, Object> startSendRtp(String app, String stream, String ssrc, String host, int port, boolean udp) {
        return post("/index/api/startSendRtp", Map.of(
                "vhost", "__defaultVhost__",
                "app", app,
                "stream", stream,
                "ssrc", ssrc,
                "dst_url", host,
                "dst_port", port,
                "is_udp", udp ? 1 : 0,
                "use_ps", 1
        ));
    }

    public Map<String, Object> stopSendRtp(String app, String stream, String ssrc) {
        Map<String, Object> params = new HashMap<>();
        params.put("vhost", "__defaultVhost__");
        params.put("app", app);
        params.put("stream", stream);
        if (ssrc != null && !ssrc.isBlank()) {
            params.put("ssrc", ssrc);
        }
        return post("/index/api/stopSendRtp", params);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        Map<String, Object> params = new HashMap<>(payload);
        if (secret != null && !secret.isBlank()) {
            params.put("secret", secret);
        }
        Map<String, Object> response = restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(params)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null) {
            throw new IllegalStateException("Empty response from ZLM");
        }
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() != 0) {
            throw new IllegalStateException(String.valueOf(response.getOrDefault("msg", "ZLM call failed")));
        }
        return response;
    }
}
