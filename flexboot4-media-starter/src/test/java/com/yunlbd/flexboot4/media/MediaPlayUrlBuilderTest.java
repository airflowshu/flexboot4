package com.yunlbd.flexboot4.media;

import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.media.core.MediaPlayUrlBuilder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MediaPlayUrlBuilderTest {

    private final MediaPlayUrlBuilder mediaPlayUrlBuilder = new MediaPlayUrlBuilder();

    @Test
    void shouldBuildExpectedUrlsFromServer() {
        MediaServer server = MediaServer.builder()
                .baseUrl("http://media.example.com:8080")
                .playDomain("play.example.com")
                .build();

        Map<String, String> urls = mediaPlayUrlBuilder.build(server, "proxy", "camera-01");

        assertEquals("http://play.example.com:8080/proxy/camera-01.live.flv", urls.get("http-flv"));
        assertEquals("ws://play.example.com:8080/proxy/camera-01.live.flv", urls.get("ws-flv"));
        assertEquals("http://play.example.com:8080/proxy/camera-01/hls.m3u8", urls.get("hls"));
        assertEquals("rtsp://play.example.com:554/proxy/camera-01", urls.get("rtsp"));
    }
}
