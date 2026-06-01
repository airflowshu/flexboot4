package com.yunlbd.flexboot4.sms;

import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestReq;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestResult;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.mapper.Sms4jConfigMapper;
import com.yunlbd.flexboot4.service.sms.impl.Sms4jConfigServiceImpl;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Sms4jConfigServiceImplTest {

    @Test
    void disabledConfigCanBeTestedAndRecordsPassedStatus() {
        SmsBlend smsBlend = mock(SmsBlend.class);
        SmsResponse response = new SmsResponse();
        response.setSuccess(true);
        response.setData(Map.of("statusCode", "000000"));
        when(smsBlend.sendMessage(
                org.mockito.Mockito.eq("13800138000"),
                org.mockito.Mockito.eq("TPL_1"),
                org.mockito.ArgumentMatchers.<LinkedHashMap<String, String>>any()
        )).thenReturn(response);

        TestableService service = new TestableService(config(0), smsBlend);

        Sms4jConfigTestResult result = service.testConfig(
                "cfg-id",
                new Sms4jConfigTestReq("13800138000", "", Map.of("1", "123456", "2", "5"))
        );

        assertThat(result.success()).isTrue();
        assertThat(result.testStatus()).isEqualTo(Sms4jConfigTestStatus.PASSED);
        assertThat(service.recordedStatus).isEqualTo(Sms4jConfigTestStatus.PASSED);
        assertThat(service.recordedMessage).contains("000000");
    }

    @Test
    void failedSupplierResponseRecordsFailedStatus() {
        SmsBlend smsBlend = mock(SmsBlend.class);
        SmsResponse response = new SmsResponse();
        response.setSuccess(false);
        response.setData(Map.of("statusCode", "161125", "statusMsg", "参数错误"));
        when(smsBlend.sendMessage(any(), any(), org.mockito.ArgumentMatchers.<LinkedHashMap<String, String>>any()))
                .thenReturn(response);

        TestableService service = new TestableService(config(1), smsBlend);

        Sms4jConfigTestResult result = service.testConfig(
                "cfg-id",
                new Sms4jConfigTestReq("13800138000", "TPL_2", Map.of())
        );

        assertThat(result.success()).isFalse();
        assertThat(result.testStatus()).isEqualTo(Sms4jConfigTestStatus.FAILED);
        assertThat(service.recordedStatus).isEqualTo(Sms4jConfigTestStatus.FAILED);
        assertThat(service.recordedMessage).contains("161125");
    }

    @Test
    void sendExceptionRecordsFailedStatus() {
        SmsBlend smsBlend = mock(SmsBlend.class);
        when(smsBlend.sendMessage(any(), any(), org.mockito.ArgumentMatchers.<LinkedHashMap<String, String>>any()))
                .thenThrow(new IllegalStateException("签名错误"));

        TestableService service = new TestableService(config(1), smsBlend);

        Sms4jConfigTestResult result = service.testConfig(
                "cfg-id",
                new Sms4jConfigTestReq("13800138000", "TPL_2", Map.of())
        );

        assertThat(result.success()).isFalse();
        assertThat(result.testStatus()).isEqualTo(Sms4jConfigTestStatus.FAILED);
        assertThat(service.recordedStatus).isEqualTo(Sms4jConfigTestStatus.FAILED);
        assertThat(service.recordedMessage).contains("签名错误");
    }

    private static Sms4jConfig config(int status) {
        Sms4jConfig config = new Sms4jConfig();
        config.setId("cfg-id");
        config.setConfigId("sms4j-config-id");
        config.setSupplierType("cloopen");
        config.setTemplateId("TPL_1");
        config.setStatus(status);
        config.setTestStatus(Sms4jConfigTestStatus.UNTESTED);
        return config;
    }

    private static class TestableService extends Sms4jConfigServiceImpl {

        private final Sms4jConfig config;
        private final SmsBlend smsBlend;
        private String recordedMessage;
        private String recordedStatus;

        private TestableService(Sms4jConfig config, SmsBlend smsBlend) {
            this.config = config;
            this.smsBlend = smsBlend;
        }

        @Override
        public Sms4jConfig getById(java.io.Serializable id) {
            return config;
        }

        @Override
        protected SmsBlend createTestSmsBlend(Sms4jConfig config) {
            return smsBlend;
        }

        @Override
        protected void recordTestResult(String id, String status, LocalDateTime testedAt, String message) {
            this.recordedStatus = status;
            this.recordedMessage = message;
        }

        @Override
        public Sms4jConfigMapper getMapper() {
            return mock(Sms4jConfigMapper.class);
        }
    }
}
