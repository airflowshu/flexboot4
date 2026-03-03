-- ============================================================
-- sms4j_config 表：短信厂商配置（flexboot4-sms4j-starter）
-- 支持多厂商、多实例，通过 config_id 与 sms4j 框架对接
-- 公共字段显式建列；厂商特有参数通过 ext_params(JSONB) 扩展
-- ============================================================

CREATE TABLE IF NOT EXISTS sms4j_config
(
    id                VARCHAR(32)  NOT NULL PRIMARY KEY,
    config_name       VARCHAR(100) NOT NULL DEFAULT '',
    supplier_type     VARCHAR(50)  NOT NULL DEFAULT '',
    config_id         VARCHAR(64)  NOT NULL DEFAULT '',
    access_key_id     VARCHAR(256) NOT NULL DEFAULT '',
    access_key_secret VARCHAR(256) NOT NULL DEFAULT '',
    signature         VARCHAR(100)          DEFAULT '',
    template_id       VARCHAR(100)          DEFAULT '',
    sdk_app_id        VARCHAR(100)          DEFAULT '',
    weight            INT                   DEFAULT 1,
    is_default        SMALLINT              DEFAULT 0,
    ext_params        JSONB,
    status            SMALLINT     NOT NULL DEFAULT 1,
    -- BaseEntity 公共字段
    create_by         VARCHAR(32)           DEFAULT '',
    create_time       TIMESTAMP             DEFAULT now(),
    last_modify_by    VARCHAR(32)           DEFAULT '',
    last_modify_time  TIMESTAMP             DEFAULT now(),
    del_flag          INT                   DEFAULT 0,
    version           BIGINT                DEFAULT 0,
    remark            VARCHAR(500)          DEFAULT ''
);

COMMENT ON TABLE sms4j_config IS '短信厂商配置表';
COMMENT ON COLUMN sms4j_config.config_name IS '配置名称，管理员自定义，如：阿里云-主账号';
COMMENT ON COLUMN sms4j_config.supplier_type IS '厂商类型：alibaba/tencent/huawei/jdcloud/yunpian/cloopen 等，对应 sms4j factory 标识';
COMMENT ON COLUMN sms4j_config.config_id IS 'sms4j 框架内部唯一标识，同一厂商可配多个';
COMMENT ON COLUMN sms4j_config.access_key_id IS 'AccessKeyId / AppId / 账号';
COMMENT ON COLUMN sms4j_config.access_key_secret IS 'AccessKeySecret / 密钥';
COMMENT ON COLUMN sms4j_config.signature IS '短信签名，如：【云途商城】';
COMMENT ON COLUMN sms4j_config.template_id IS '默认短信模板ID';
COMMENT ON COLUMN sms4j_config.sdk_app_id IS 'SDK AppId（腾讯云等专用）';
COMMENT ON COLUMN sms4j_config.weight IS '负载均衡权重，数值越大概率越高';
COMMENT ON COLUMN sms4j_config.is_default IS '是否默认：1-是，0-否';
COMMENT ON COLUMN sms4j_config.ext_params IS '厂商特有扩展参数 JSONB';
COMMENT ON COLUMN sms4j_config.status IS '状态：1-启用，0-禁用';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sms4j_config_config_id ON sms4j_config (config_id) WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_sms4j_config_supplier ON sms4j_config (supplier_type, status);

-- ============================================================
-- sys_menu 初始化数据：短信管理菜单
-- 请按实际 sys_menu 表主键生成规则调整 ID 值
-- parent_id 填写实际的根菜单 id（'0' 表示顶层）
-- ============================================================

-- 短信管理 - 父菜单（一级目录）
INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('sms_menu_root', '0', '/sms', 'Sms', 'LAYOUT', '短信管理', 'ant-design:message-outlined', 90,
        false, false, 1, now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

-- 短信厂商配置 - 子菜单（二级页面）
INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('sms_menu_config', 'sms_menu_root', '/sms/config', 'SmsConfig',
        '/sms/config/index', '短信厂商配置', 'ant-design:setting-outlined', 1,
        false, true, 1, now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

