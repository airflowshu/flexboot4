package com.yunlbd.flexboot4.sms;

import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import com.yunlbd.flexboot4.controller.sms.Sms4jConfigController;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigUpdateReq;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Sms4jConfigControllerTest {

    private final Sms4jConfigService service = mock(Sms4jConfigService.class);
    private final SmsSupplierConfigDataSource dataSource = mock(SmsSupplierConfigDataSource.class);
    private final Sms4jConfigController controller = new Sms4jConfigController(service, dataSource);

    @Test
    void sendingConfigChangeResetsTestStatus() {
        Sms4jConfig config = config();
        Sms4jConfigUpdateReq request = new Sms4jConfigUpdateReq();
        request.setAccessKeyId("new-ak");

        when(service.getById("cfg-id")).thenReturn(config);
        when(service.updateById(config, true)).thenReturn(true);

        controller.update("cfg-id", request);

        verify(service).resetTestStatus("cfg-id");
        verify(dataSource).reloadAll();
    }

    @Test
    void statusOnlyChangeKeepsTestStatus() {
        Sms4jConfig config = config();
        Sms4jConfigUpdateReq request = new Sms4jConfigUpdateReq();
        request.setStatus(0);

        when(service.getById("cfg-id")).thenReturn(config);
        when(service.updateById(config, true)).thenReturn(true);

        controller.update("cfg-id", request);

        verify(service, never()).resetTestStatus("cfg-id");
        verify(dataSource).reloadAll();
    }

    private static Sms4jConfig config() {
        Sms4jConfig config = new Sms4jConfig();
        config.setId("cfg-id");
        config.setConfigId("sms4j-config-id");
        config.setConfigName("容联云主账号");
        config.setSupplierType("cloopen");
        config.setAccessKeyId("old-ak");
        config.setAccessKeySecret("old-sk");
        config.setSignature("签名");
        config.setTemplateId("TPL_1");
        config.setSdkAppId("APP_1");
        config.setStatus(1);
        config.setWeight(1);
        config.setIsDefault(1);
        config.setTestStatus(Sms4jConfigTestStatus.PASSED);
        return config;
    }
}
