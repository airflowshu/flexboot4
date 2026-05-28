-- 手机号验证码登录配置与手机号唯一性约束。
-- 当前项目仍在开发阶段，若存在重复手机号，本脚本会中断并提示先清理数据。

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_user
        WHERE del_flag = 0
          AND phone IS NOT NULL
          AND btrim(phone) <> ''
        GROUP BY phone
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'sys_user.phone has duplicate active values. Clean duplicate phone data before enabling SMS login.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_phone_alive
    ON sys_user (phone)
    WHERE del_flag = 0
      AND phone IS NOT NULL
      AND btrim(phone) <> '';

INSERT INTO sys_config (id, config_key, config_value, config_type, description, status,
                        create_time, last_modify_time, del_flag, version, remark)
VALUES (
    'auth_login_options',
    'auth.login.options',
    '{"methods":{"password":{"enabled":true},"sms":{"enabled":false,"codeLength":4,"cooldownSeconds":60},"qrcode":{"enabled":false},"thirdParty":{"enabled":false,"providers":[]},"register":{"enabled":false},"forgetPassword":{"enabled":true}}}',
    'JSON',
    '登录页认证方式开关配置',
    1,
    now(),
    now(),
    0,
    0,
    '短信登录启用前需配置短信厂商与用户手机号'
)
ON CONFLICT (id) DO UPDATE SET
    config_key = EXCLUDED.config_key,
    config_type = EXCLUDED.config_type,
    description = EXCLUDED.description,
    last_modify_time = now();

INSERT INTO sys_config (id, config_key, config_value, config_type, description, status,
                        create_time, last_modify_time, del_flag, version, remark)
VALUES (
    'auth_sms_template_id',
    'auth.sms.templateId',
    '1',
    'STRING',
    '手机号登录短信模板ID；容联云测试模板默认 1',
    1,
    now(),
    now(),
    0,
    0,
    '容联云测试模板参数：{1}=验证码（1-4位数字），{2}=有效分钟数'
)
ON CONFLICT (id) DO UPDATE SET
    config_key = EXCLUDED.config_key,
    config_type = EXCLUDED.config_type,
    description = EXCLUDED.description,
    last_modify_time = now();

INSERT INTO sys_config (id, config_key, config_value, config_type, description, status,
                        create_time, last_modify_time, del_flag, version, remark)
VALUES (
    'auth_sms_config_id',
    'auth.sms.configId',
    '',
    'STRING',
    '手机号登录短信发送指定的 sms4j configId；留空时使用默认启用配置',
    1,
    now(),
    now(),
    0,
    0,
    ''
)
ON CONFLICT (id) DO UPDATE SET
    config_key = EXCLUDED.config_key,
    config_type = EXCLUDED.config_type,
    description = EXCLUDED.description,
    last_modify_time = now();
