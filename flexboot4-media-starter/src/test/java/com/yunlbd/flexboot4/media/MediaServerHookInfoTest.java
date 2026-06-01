package com.yunlbd.flexboot4.media;

import com.yunlbd.flexboot4.entity.media.MediaServer;
import com.yunlbd.flexboot4.media.config.MediaRestClientFactory;
import com.yunlbd.flexboot4.media.core.MediaPlayUrlBuilder;
import com.yunlbd.flexboot4.media.dto.MediaServerHookInfo;
import com.yunlbd.flexboot4.service.media.MediaServerService;
import com.yunlbd.flexboot4.service.media.impl.MediaServerServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class MediaServerHookInfoTest {

    @Test
    void shouldBuildStablePerServerHookUrls() {
        MediaServerService service = spy(new MediaServerServiceImpl(
                properties(),
                new MediaRestClientFactory(RestClient.builder()),
                new MediaPlayUrlBuilder()
        ));
        MediaServer server = MediaServer.builder()
                .id("server-1")
                .serverName("zlm-1")
                .baseUrl("http://127.0.0.1:8080")
                .hookSecret("hook-secret")
                .build();
        doReturn(server).when(service).getById("server-1");

        MediaServerHookInfo hookInfo = service.buildHookInfo("server-1");

        assertThat(hookInfo.onStreamChanged())
                .isEqualTo("http://flexboot.example.com/api/admin/media/zlm/hook/server-1/on_stream_changed");
        assertThat(hookInfo.onServerKeepalive())
                .isEqualTo("http://flexboot.example.com/api/admin/media/zlm/hook/server-1/on_server_keepalive");
        assertThat(hookInfo.adminParams()).isEqualTo("secret=hook-secret");
        assertThat(hookInfo.urls())
                .containsEntry("hook.on_rtp_server_timeout",
                        "http://flexboot.example.com/api/admin/media/zlm/hook/server-1/on_rtp_server_timeout");
    }

    private MediaProperties properties() {
        return new MediaProperties(
                true,
                "http://flexboot.example.com/",
                "http-flv",
                "media_snapshot",
                2,
                8,
                1024,
                "X-Media-Hook-Signature",
                300,
                true,
                30000,
                30000,
                180,
                180,
                30,
                300,
                true
        );
    }
}
