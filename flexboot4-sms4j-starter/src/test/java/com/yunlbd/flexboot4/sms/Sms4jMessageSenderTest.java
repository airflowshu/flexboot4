package com.yunlbd.flexboot4.sms;

import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import com.yunlbd.flexboot4.service.sms.Sms4jMessageSender;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Sms4jMessageSenderTest {

    private final Sms4jConfigService configService = mock(Sms4jConfigService.class);
    private final SmsSupplierConfigDataSource dataSource = mock(SmsSupplierConfigDataSource.class);
    private final Sms4jMessageSender sender = new Sms4jMessageSender(configService, dataSource);

    @Test
    void missingConfigFailsBeforeSending() {
        when(configService.listEnabledConfigs()).thenReturn(List.of());

        assertThatThrownBy(() -> sender.send(request(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未找到可用短信配置");
    }

    @Test
    void unknownExplicitConfigFailsClearly() {
        when(configService.listEnabledConfigs()).thenReturn(List.of(config("default", 1)));

        assertThatThrownBy(() -> sender.send(request("missing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("指定短信配置不存在或未启用");
    }

    @Test
    void uninitializedBlendTriggersReloadBeforeFailingClearly() {
        when(configService.listEnabledConfigs()).thenReturn(List.of(config("default", 1)));

        try (MockedStatic<SmsFactory> smsFactory = mockStatic(SmsFactory.class)) {
            smsFactory.when(() -> SmsFactory.getSmsBlend("default")).thenReturn(null);

            assertThatThrownBy(() -> sender.send(request("default")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("短信通道未初始化");

            verify(dataSource).reload("default");
            smsFactory.verify(() -> SmsFactory.getSmsBlend("default"), org.mockito.Mockito.times(2));
        }
    }

    @Test
    void failedSupplierResponseFailsClearly() {
        when(configService.listEnabledConfigs()).thenReturn(List.of(config("default", 1)));
        SmsBlend smsBlend = mock(SmsBlend.class);
        SmsResponse response = new SmsResponse();
        response.setSuccess(false);
        response.setConfigId("default");
        response.setData("{\"statusCode\":\"161125\",\"statusMsg\":\"请输入1到4位的数字\"}");
        when(smsBlend.sendMessage(
                org.mockito.Mockito.eq("13800138000"),
                org.mockito.Mockito.eq("1"),
                org.mockito.ArgumentMatchers.<LinkedHashMap<String, String>>any()
        ))
                .thenReturn(response);

        try (MockedStatic<SmsFactory> smsFactory = mockStatic(SmsFactory.class)) {
            smsFactory.when(() -> SmsFactory.getSmsBlend("default")).thenReturn(smsBlend);

            assertThatThrownBy(() -> sender.send(request("default")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("短信发送失败")
                    .hasMessageContaining("161125");
        }
    }

    @Test
    void successfulSupplierResponseDoesNotFail() {
        when(configService.listEnabledConfigs()).thenReturn(List.of(config("default", 1)));
        SmsBlend smsBlend = mock(SmsBlend.class);
        SmsResponse response = new SmsResponse();
        response.setSuccess(true);
        response.setConfigId("default");
        response.setData("{\"statusCode\":\"000000\"}");
        when(smsBlend.sendMessage(
                org.mockito.Mockito.eq("13800138000"),
                org.mockito.Mockito.eq("1"),
                org.mockito.ArgumentMatchers.<LinkedHashMap<String, String>>any()
        ))
                .thenReturn(response);

        try (MockedStatic<SmsFactory> smsFactory = mockStatic(SmsFactory.class)) {
            smsFactory.when(() -> SmsFactory.getSmsBlend("default")).thenReturn(smsBlend);

            sender.send(request("default"));
        }
    }

    private static SmsMessageRequest request(String configId) {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("1", "123456");
        params.put("2", "5");
        return new SmsMessageRequest("13800138000", "1", params, configId);
    }

    private static Sms4jConfig config(String configId, int isDefault) {
        Sms4jConfig config = new Sms4jConfig();
        config.setConfigId(configId);
        config.setSupplierType("cloopen");
        config.setTemplateId("1");
        config.setIsDefault(isDefault);
        return config;
    }
}
