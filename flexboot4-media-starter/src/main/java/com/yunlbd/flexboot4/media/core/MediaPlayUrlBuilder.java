package com.yunlbd.flexboot4.media.core;

import com.yunlbd.flexboot4.entity.media.MediaServer;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MediaPlayUrlBuilder {

    public Map<String, String> build(MediaServer server, String app, String stream) {
        URI baseUri = URI.create(server.getBaseUrl());
        String host = firstNonBlank(server.getPlayDomain(), server.getPublicHost(), baseUri.getHost(), "localhost");
        String httpScheme = "https".equalsIgnoreCase(baseUri.getScheme()) ? "https" : "http";
        String wsScheme = "https".equalsIgnoreCase(httpScheme) ? "wss" : "ws";
        int httpPort = baseUri.getPort() > 0 ? baseUri.getPort() : ("https".equalsIgnoreCase(httpScheme) ? 443 : 80);

        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("http-flv", httpScheme + "://" + hostPort(host, httpPort, "https".equalsIgnoreCase(httpScheme) ? 443 : 80) + "/" + app + "/" + stream + ".live.flv");
        urls.put("ws-flv", wsScheme + "://" + hostPort(host, httpPort, "wss".equalsIgnoreCase(wsScheme) ? 443 : 80) + "/" + app + "/" + stream + ".live.flv");
        urls.put("hls", httpScheme + "://" + hostPort(host, httpPort, "https".equalsIgnoreCase(httpScheme) ? 443 : 80) + "/" + app + "/" + stream + "/hls.m3u8");
        urls.put("http-fmp4", httpScheme + "://" + hostPort(host, httpPort, "https".equalsIgnoreCase(httpScheme) ? 443 : 80) + "/" + app + "/" + stream + ".live.mp4");
        urls.put("ws-fmp4", wsScheme + "://" + hostPort(host, httpPort, "wss".equalsIgnoreCase(wsScheme) ? 443 : 80) + "/" + app + "/" + stream + ".live.mp4");
        urls.put("rtsp", "rtsp://" + host + ":554/" + app + "/" + stream);
        urls.put("rtmp", "rtmp://" + host + ":1935/" + app + "/" + stream);
        return urls;
    }

    private String hostPort(String host, int port, int defaultPort) {
        return port == defaultPort ? host : host + ":" + port;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
