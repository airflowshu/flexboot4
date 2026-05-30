-- 短信验证码发送 IP 维度限流配置。

INSERT INTO sys_config (id, config_key, config_value, config_type, description, status,
                        create_time, last_modify_time, del_flag, version, remark)
VALUES (
    'auth_sms_ip_hourly_limit',
    'auth.sms.ipHourlyLimit',
    '30',
    'NUMBER',
    '手机号登录短信验证码同一 IP 每小时最大发送请求次数',
    1,
    now(),
    now(),
    0,
    0,
    '用于限制同一 IP 横向请求多个手机号消耗短信额度'
)
ON CONFLICT (id) DO UPDATE SET
    config_key = EXCLUDED.config_key,
    config_type = EXCLUDED.config_type,
    description = EXCLUDED.description,
    last_modify_time = now();

INSERT INTO sys_config (id, config_key, config_value, config_type, description, status,
                        create_time, last_modify_time, del_flag, version, remark)
VALUES (
    'auth_sms_ip_daily_limit',
    'auth.sms.ipDailyLimit',
    '100',
    'NUMBER',
    '手机号登录短信验证码同一 IP 每日最大发送请求次数',
    1,
    now(),
    now(),
    0,
    0,
    '用于限制同一 IP 横向请求多个手机号消耗短信额度'
)
ON CONFLICT (id) DO UPDATE SET
    config_key = EXCLUDED.config_key,
    config_type = EXCLUDED.config_type,
    description = EXCLUDED.description,
    last_modify_time = now();
