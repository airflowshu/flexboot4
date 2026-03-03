package com.yunlbd.flexboot4.sms;

import com.yunlbd.flexboot4.BootstrapApplication;
import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 容联云真实发短信联调测试（默认关闭）。
 * <p>
 * 使用 IDEA 的 Run Configuration（推荐）
 * 1. 创建测试运行配置
 * 在 IDEA 中打开 CloopenSmsRealSendTest.java
 * 在类名或测试方法上右键 → 选择 "Modify Run Configuration..."（或者"More Run/Debug" → "Modify Run Configuration..."）
 * 在弹出的配置窗口中找到 "Environment variables" 字段
 * 点击右侧的 文件夹图标 或 "..." 按钮
 * 在环境变量编辑窗口中添加以下变量（点击 + 号逐个添加）：
 * <p>SMS_REAL_SEND_ENABLED=true </p>
 * <p>SMS_TEST_PHONE=你的手机号（例如：1515608xxxx）</p>
 * <p>SMS_TEST_PARAM1=1234（验证码，对应模板 {1}）</p>
 * <p>SMS_TEST_PARAM2=5（有效分钟数，对应模板 {2}）</p>
 * 点击 OK 保存配置
 * 点击 Run 或 Debug 按钮执行测试
 *
 *
 * <p>启用方式：设置环境变量 SMS_REAL_SEND_ENABLED=true 后再执行该测试。</p>
 */
@SpringBootTest(classes = BootstrapApplication.class)
@EnabledIfEnvironmentVariable(named = "SMS_REAL_SEND_ENABLED", matches = "(?i)true")
class CloopenSmsRealSendTest {

    private static final String TARGET_PHONE_ENV = "SMS_TEST_PHONE";
    private static final String TEMPLATE_PARAM1_ENV = "SMS_TEST_PARAM1";
    private static final String TEMPLATE_PARAM2_ENV = "SMS_TEST_PARAM2";

    @Autowired
    private Sms4jConfigService sms4jConfigService;

    @Autowired
    private SmsSupplierConfigDataSource smsDataSource;

    @BeforeEach
    void setUp() {
        // 手动触发 sms4j 配置加载，确保测试环境下 SmsBlend 实例已初始化
        smsDataSource.reloadAll();
    }

    @Test
    void shouldSendSmsByCloopenConfig() {
        // 容联云免费测试模板要求：{1}验证码需6-20位数字，{2}有效分钟数
        String phone = getEnvOrDefault(TARGET_PHONE_ENV, "15156080627");
        String param1 = getEnvOrDefault(TEMPLATE_PARAM1_ENV, "888888");  // 6位验证码
        String param2 = getEnvOrDefault(TEMPLATE_PARAM2_ENV, "5");       // 有效分钟数

        Sms4jConfig cloopenConfig = sms4jConfigService.listEnabledConfigs().stream()
                .filter(c -> "cloopen".equalsIgnoreCase(c.getSupplierType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到已启用的 cloopen 配置，请先在短信厂商配置中新增并启用。"));

        var smsBlend = SmsFactory.getSmsBlend(cloopenConfig.getConfigId());
        assertThat(smsBlend)
                .as("未获取到 SmsBlend，configId=%s", cloopenConfig.getConfigId())
                .isNotNull();

        LinkedHashMap<String, String> templateParams = new LinkedHashMap<>();
        templateParams.put("1", param1);
        templateParams.put("2", param2);

        // 容联云免费模板(模板ID=1)包含 {1}、{2} 两个占位符，因此传两个模板参数。
        Object response = smsBlend.sendMessage(phone, cloopenConfig.getTemplateId(), templateParams);
        assertThat(response).isNotNull();

        // 便于联调时在测试输出中看到回执信息
        System.out.println("[SMS-REAL-TEST] provider=" + cloopenConfig.getSupplierType()
                + ", configId=" + cloopenConfig.getConfigId()
                + ", phone=" + phone
                + ", param1=" + param1
                + ", param2=" + param2
                + ", response=" + response);
    }


    /**
     * 优先使用环境变量，如果未设置则使用默认值（便于本地开发调试）
     */
    private String getEnvOrDefault(String key, String defaultValue) {
        String env = System.getenv(key);
        return (env != null && !env.isBlank()) ? env : defaultValue;
    }
}
