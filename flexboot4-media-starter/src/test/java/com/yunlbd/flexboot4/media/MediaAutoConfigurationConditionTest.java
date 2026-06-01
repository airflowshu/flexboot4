package com.yunlbd.flexboot4.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.mapper.MediaCascadeBindingMapper;
import com.yunlbd.flexboot4.mapper.MediaCascadePlatformMapper;
import com.yunlbd.flexboot4.mapper.MediaChannelMapper;
import com.yunlbd.flexboot4.mapper.MediaDeviceMapper;
import com.yunlbd.flexboot4.mapper.MediaGatewayMapper;
import com.yunlbd.flexboot4.mapper.MediaScreenMapper;
import com.yunlbd.flexboot4.mapper.MediaScreenSlotMapper;
import com.yunlbd.flexboot4.mapper.MediaServerMapper;
import com.yunlbd.flexboot4.mapper.MediaStreamSessionMapper;
import com.yunlbd.flexboot4.media.config.MediaRestClientFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MediaAutoConfigurationConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MediaAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(MediaCascadeBindingMapper.class, () -> mock(MediaCascadeBindingMapper.class))
            .withBean(MediaCascadePlatformMapper.class, () -> mock(MediaCascadePlatformMapper.class))
            .withBean(MediaChannelMapper.class, () -> mock(MediaChannelMapper.class))
            .withBean(MediaDeviceMapper.class, () -> mock(MediaDeviceMapper.class))
            .withBean(MediaGatewayMapper.class, () -> mock(MediaGatewayMapper.class))
            .withBean(MediaScreenMapper.class, () -> mock(MediaScreenMapper.class))
            .withBean(MediaScreenSlotMapper.class, () -> mock(MediaScreenSlotMapper.class))
            .withBean(MediaServerMapper.class, () -> mock(MediaServerMapper.class))
            .withBean(MediaStreamSessionMapper.class, () -> mock(MediaStreamSessionMapper.class));

    @Test
    void shouldNotRegisterRuntimeBeansWhenMediaDisabled() {
        contextRunner
                .withPropertyValues("media.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MediaRestClientFactory.class));
    }

    @Test
    void shouldRegisterRuntimeBeansWhenMediaEnabled() {
        contextRunner
                .withPropertyValues(
                        "media.enabled=true",
                        "media.callback-base-url=http://localhost:8080",
                        "media.default-play-protocol=http-flv",
                        "media.snapshot-biz-type=media_snapshot",
                        "media.gateway-core-threads=2",
                        "media.gateway-max-threads=8",
                        "media.gateway-queue-capacity=1024",
                        "media.hook-secret-header=X-Media-Hook-Signature",
                        "media.hook-timestamp-tolerance-seconds=300",
                        "media.runtime-check-enabled=false",
                        "media.runtime-check-initial-delay-millis=30000",
                        "media.runtime-check-fixed-delay-millis=30000",
                        "media.server-hook-timeout-seconds=180",
                        "media.device-keepalive-timeout-seconds=180",
                        "media.pending-session-timeout-seconds=30",
                        "media.streaming-session-timeout-seconds=300",
                        "media.gateway-auto-recover=true"
                )
                .run(context -> assertThat(context).hasSingleBean(MediaRestClientFactory.class));
    }
}
