-- ============================================================
-- FlexBoot4 Admin Starter initialization (PostgreSQL)
-- Base tables and default data owned by the admin module.
-- Later changes are managed by db/flexboot4-migration/admin/postgresql.
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_dept (
    id VARCHAR(64) PRIMARY KEY,
    dept_name VARCHAR(100) NOT NULL,
    order_no INTEGER DEFAULT 0,
    status INTEGER DEFAULT 1,
    parent_id VARCHAR(64),
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_dept IS '部门表';
COMMENT ON COLUMN sys_dept.dept_name IS '部门名称';
COMMENT ON COLUMN sys_dept.order_no IS '排序号';
COMMENT ON COLUMN sys_dept.status IS '状态：0-禁用，1-启用';
COMMENT ON COLUMN sys_dept.parent_id IS '父部门ID';

CREATE INDEX IF NOT EXISTS idx_sys_dept_parent_id ON sys_dept(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_dept_status ON sys_dept(status);


CREATE TABLE IF NOT EXISTS sys_menu (
    id VARCHAR(64) PRIMARY KEY,
    parent_id VARCHAR(64),
    path VARCHAR(255),
    name VARCHAR(100),
    component VARCHAR(255),
    redirect VARCHAR(255),
    title VARCHAR(120),
    icon VARCHAR(120),
    order_no INTEGER DEFAULT 0,
    hide_in_menu BOOLEAN DEFAULT FALSE,
    keep_alive BOOLEAN DEFAULT FALSE,
    active_icon VARCHAR(120),
    badge VARCHAR(120),
    badge_type VARCHAR(32),
    badge_variants VARCHAR(32),
    link VARCHAR(500),
    iframe_src VARCHAR(500),
    affix_tab BOOLEAN DEFAULT FALSE,
    hide_children_in_menu BOOLEAN DEFAULT FALSE,
    hide_in_breadcrumb BOOLEAN DEFAULT FALSE,
    hide_in_tab BOOLEAN DEFAULT FALSE,
    menu_visible_with_forbidden BOOLEAN DEFAULT FALSE,
    authority VARCHAR(500),
    auth_code VARCHAR(120),
    type VARCHAR(32) DEFAULT 'menu',
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_menu IS '菜单与按钮权限表';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID，根节点为NULL';
COMMENT ON COLUMN sys_menu.path IS '前端路由路径';
COMMENT ON COLUMN sys_menu.name IS '前端路由名称';
COMMENT ON COLUMN sys_menu.component IS '前端组件路径';
COMMENT ON COLUMN sys_menu.auth_code IS '权限编码';
COMMENT ON COLUMN sys_menu.type IS '菜单类型：catalog/menu/button/embedded/link';

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_auth_code ON sys_menu(auth_code);
CREATE INDEX IF NOT EXISTS idx_sys_menu_status ON sys_menu(status);


CREATE TABLE IF NOT EXISTS sys_role (
    id VARCHAR(64) PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL,
    role_value VARCHAR(100) NOT NULL,
    status INTEGER DEFAULT 1,
    description VARCHAR(500),
    order_no INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_name IS '角色名称';
COMMENT ON COLUMN sys_role.role_value IS '角色值';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_value_alive
    ON sys_role(role_value)
    WHERE del_flag = 0;


CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    profile_file_id VARCHAR(64),
    email VARCHAR(255),
    phone VARCHAR(32),
    gender VARCHAR(32),
    dept_id VARCHAR(64),
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.username IS '登录名';
COMMENT ON COLUMN sys_user.password IS 'BCrypt密码';
COMMENT ON COLUMN sys_user.profile_file_id IS '头像文件ID';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username_alive
    ON sys_user(username)
    WHERE del_flag = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_phone_alive
    ON sys_user(phone)
    WHERE del_flag = 0
      AND phone IS NOT NULL
      AND btrim(phone) <> '';
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_email_alive
    ON sys_user(lower(btrim(email)))
    WHERE del_flag = 0
      AND email IS NOT NULL
      AND btrim(email) <> '';
CREATE INDEX IF NOT EXISTS idx_sys_user_dept_id ON sys_user(dept_id);


CREATE TABLE IF NOT EXISTS sys_role_menu (
    id VARCHAR(64) PRIMARY KEY,
    role_id VARCHAR(64) NOT NULL,
    menu_id VARCHAR(64) NOT NULL,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_menu_role_menu
    ON sys_role_menu(role_id, menu_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);


CREATE TABLE IF NOT EXISTS sys_user_role (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    role_id VARCHAR(64) NOT NULL,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_role_user_role
    ON sys_user_role(user_id, role_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_role_role_id ON sys_user_role(role_id);


CREATE TABLE IF NOT EXISTS sys_file (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64),
    biz_type VARCHAR(64),
    biz_id VARCHAR(64),
    file_name VARCHAR(255) NOT NULL,
    file_ext VARCHAR(32),
    mime_type VARCHAR(120),
    file_size BIGINT DEFAULT 0,
    file_hash VARCHAR(128),
    storage_type VARCHAR(32) DEFAULT 'LOCAL',
    bucket_name VARCHAR(128),
    object_key VARCHAR(500),
    ai_status VARCHAR(32),
    ai_parse_status VARCHAR(32),
    ai_embed_status VARCHAR(32),
    chunk_count INTEGER DEFAULT 0,
    token_estimate INTEGER DEFAULT 0,
    embedding_model VARCHAR(120),
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_file IS '系统文件表';
COMMENT ON COLUMN sys_file.file_hash IS '文件内容哈希';
COMMENT ON COLUMN sys_file.object_key IS '存储对象Key';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_file_hash_alive
    ON sys_file(file_hash)
    WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_sys_file_biz ON sys_file(biz_type, biz_id);


CREATE TABLE IF NOT EXISTS sys_config (
    id VARCHAR(64) PRIMARY KEY,
    config_key VARCHAR(160) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(32) DEFAULT 'STRING',
    description VARCHAR(500),
    status INTEGER DEFAULT 1,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_config IS '系统参数配置表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_key_alive
    ON sys_config(config_key)
    WHERE del_flag = 0;


CREATE TABLE IF NOT EXISTS sys_dict_type (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status INTEGER DEFAULT 1,
    order_no INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_dict_type IS '字典类型表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_type_code_alive
    ON sys_dict_type(code)
    WHERE del_flag = 0;


CREATE TABLE IF NOT EXISTS sys_dict_item (
    id VARCHAR(64) PRIMARY KEY,
    type_id VARCHAR(64) NOT NULL,
    item_code VARCHAR(100),
    item_text VARCHAR(120) NOT NULL,
    item_value VARCHAR(120) NOT NULL,
    status INTEGER DEFAULT 1,
    order_no INTEGER DEFAULT 0,
    parent_code VARCHAR(100),
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_dict_item IS '字典项表';

CREATE INDEX IF NOT EXISTS idx_sys_dict_item_type_id ON sys_dict_item(type_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_item_type_value_alive
    ON sys_dict_item(type_id, item_value)
    WHERE del_flag = 0;


CREATE TABLE IF NOT EXISTS sys_oper_log (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    event_id VARCHAR(128),
    title VARCHAR(50) DEFAULT '',
    business_type INTEGER DEFAULT 0,
    method VARCHAR(160) DEFAULT '',
    request_method VARCHAR(16) DEFAULT '',
    operator_type INTEGER DEFAULT 0,
    oper_name VARCHAR(100) DEFAULT '',
    oper_user_id VARCHAR(64) DEFAULT '',
    dept_id VARCHAR(64),
    oper_url VARCHAR(500) DEFAULT '',
    oper_ip VARCHAR(128) DEFAULT '',
    oper_location VARCHAR(255) DEFAULT '',
    terminal JSONB,
    oper_param JSONB,
    json_result JSONB,
    status INTEGER DEFAULT 0,
    error_msg TEXT,
    oper_time TIMESTAMP,
    cost_time BIGINT DEFAULT 0,
    ext_params JSONB,
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_oper_log IS '操作日志记录';
COMMENT ON COLUMN sys_oper_log.event_id IS '外部事件幂等ID';
COMMENT ON COLUMN sys_oper_log.terminal IS '操作终端信息';

CREATE INDEX IF NOT EXISTS idx_sys_oper_log_bt ON sys_oper_log(business_type, status, oper_time);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_ot ON sys_oper_log(oper_time);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_oper_log_event_id
    ON sys_oper_log(event_id)
    WHERE event_id IS NOT NULL AND event_id <> '';

DO $$
DECLARE
    current_quarter_table TEXT := format('sys_oper_log_%s_q%s',
                                         EXTRACT(YEAR FROM CURRENT_DATE)::INT,
                                         EXTRACT(QUARTER FROM CURRENT_DATE)::INT);
BEGIN
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I (LIKE sys_oper_log INCLUDING ALL)', current_quarter_table);
END;
$$;


CREATE TABLE IF NOT EXISTS sys_user_mfa (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    secret_ciphertext TEXT NOT NULL,
    device_name VARCHAR(120),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    confirmed_at TIMESTAMP,
    last_used_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_user_mfa IS '用户多因素认证设备';

CREATE INDEX IF NOT EXISTS idx_sys_user_mfa_user_type_alive
    ON sys_user_mfa(user_id, type)
    WHERE del_flag = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_mfa_enabled_totp
    ON sys_user_mfa(user_id, type)
    WHERE del_flag = 0
      AND enabled = TRUE
      AND type = 'TOTP';


CREATE TABLE IF NOT EXISTS sys_version_log (
    id VARCHAR(64) PRIMARY KEY,
    version_no VARCHAR(64) NOT NULL,
    release_date TIMESTAMP,
    type VARCHAR(32),
    title VARCHAR(200),
    description TEXT,
    status INTEGER DEFAULT 0,
    features JSONB,
    fixes JSONB,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE sys_version_log IS '系统版本日志表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_version_log_no_alive
    ON sys_version_log(version_no)
    WHERE del_flag = 0;


CREATE TABLE IF NOT EXISTS ai_api_key (
    id VARCHAR(64) PRIMARY KEY,
    key_name VARCHAR(120) NOT NULL,
    api_key VARCHAR(160) NOT NULL,
    user_id VARCHAR(64),
    status INTEGER DEFAULT 1,
    quote BIGINT DEFAULT 0,
    used BIGINT DEFAULT 0,
    model_scope TEXT,
    expires_at TIMESTAMP,
    last_used_time TIMESTAMP,
    notes TEXT,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT now(),
    last_modify_time TIMESTAMP DEFAULT now(),
    create_by VARCHAR(64),
    last_modify_by VARCHAR(64),
    remark VARCHAR(500)
);

COMMENT ON TABLE ai_api_key IS 'AI API Key表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_ai_api_key_alive
    ON ai_api_key(api_key)
    WHERE del_flag = 0;
CREATE INDEX IF NOT EXISTS idx_ai_api_key_user_id ON ai_api_key(user_id);


INSERT INTO sys_dept (id, dept_name, order_no, status, parent_id, create_time, last_modify_time, del_flag, version)
VALUES ('1', '总公司', 1, 1, NULL, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    dept_name = EXCLUDED.dept_name,
    order_no = EXCLUDED.order_no,
    status = EXCLUDED.status,
    parent_id = EXCLUDED.parent_id,
    last_modify_time = now();

INSERT INTO sys_role (id, role_name, role_value, status, description, order_no, create_time, last_modify_time, del_flag, version)
VALUES
('1', '超级管理员', 'super', 1, '系统内置超级管理员角色', 1, now(), now(), 0, 0),
('2', '管理员', 'admin', 1, '系统内置管理员角色', 2, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name,
    role_value = EXCLUDED.role_value,
    status = EXCLUDED.status,
    description = EXCLUDED.description,
    order_no = EXCLUDED.order_no,
    last_modify_time = now();

INSERT INTO sys_user (id, username, password, real_name, dept_id, status, create_time, last_modify_time, del_flag, version)
VALUES
('1', 'super', '$2a$10$R96mE..coy6eWYzXv4p2QunDJjnfKnPr5gFjKMPhHBGRMARSjdAqu', '超级管理员', '1', 1, now(), now(), 0, 0),
('2', 'admin', '$2a$10$rsC7h5AjnyALa6vQlP7n..i45rgVqdNaFV3Lui.TnnyzfIuAw9dba', '管理员', '1', 1, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    real_name = EXCLUDED.real_name,
    dept_id = EXCLUDED.dept_id,
    status = EXCLUDED.status,
    last_modify_time = now();

INSERT INTO sys_user_role (id, user_id, role_id, create_time, last_modify_time, del_flag, version)
VALUES
('sys_user_role_super', '1', '1', now(), now(), 0, 0),
('sys_user_role_admin', '2', '2', now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_config (id, config_key, config_value, config_type, description, status,
                        create_time, last_modify_time, del_flag, version, remark)
VALUES
('auth_login_options', 'auth.login.options',
 '{"methods":{"password":{"enabled":true},"sms":{"enabled":false,"codeLength":4,"cooldownSeconds":60},"qrcode":{"enabled":false},"thirdParty":{"enabled":false,"providers":[]},"register":{"enabled":false},"forgetPassword":{"enabled":true}}}',
 'JSON', '登录页认证方式开关配置', 1, now(), now(), 0, 0, '短信登录启用前需配置短信厂商与用户手机号'),
('auth_sms_template_id', 'auth.sms.templateId', '1', 'STRING',
 '手机号登录短信模板ID；容联云测试模板默认 1', 1, now(), now(), 0, 0,
 '容联云测试模板参数：{1}=验证码（1-4位数字），{2}=有效分钟数'),
('auth_sms_config_id', 'auth.sms.configId', '', 'STRING',
 '手机号登录短信发送指定的 sms4j configId；留空时使用默认启用配置', 1, now(), now(), 0, 0, ''),
('auth_sms_ip_hourly_limit', 'auth.sms.ipHourlyLimit', '30', 'NUMBER',
 '手机号登录短信验证码同一 IP 每小时最大发送请求次数', 1, now(), now(), 0, 0,
 '用于限制同一 IP 横向请求多个手机号消耗短信额度'),
('auth_sms_ip_daily_limit', 'auth.sms.ipDailyLimit', '100', 'NUMBER',
 '手机号登录短信验证码同一 IP 每日最大发送请求次数', 1, now(), now(), 0, 0,
 '用于限制同一 IP 横向请求多个手机号消耗短信额度')
ON CONFLICT (id) DO UPDATE SET
    config_key = EXCLUDED.config_key,
    config_type = EXCLUDED.config_type,
    description = EXCLUDED.description,
    last_modify_time = now();

INSERT INTO sys_dict_type (id, code, name, status, order_no, create_time, last_modify_time, del_flag, version)
VALUES
('dict_status', 'status', '通用状态', 1, 1, now(), now(), 0, 0),
('dict_gender', 'gender', '性别', 1, 2, now(), now(), 0, 0),
('dict_business_type', 'business_type', '操作业务类型', 1, 3, now(), now(), 0, 0),
('dict_operator_type', 'operator_type', '操作人员类型', 1, 4, now(), now(), 0, 0),
('dict_version_type', 'version_type', '版本类型', 1, 5, now(), now(), 0, 0),
('dict_sms_supplier_type', 'sms_supplier_type', '短信厂商类型', 1, 6, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    order_no = EXCLUDED.order_no,
    last_modify_time = now();

INSERT INTO sys_dict_item (id, type_id, item_code, item_text, item_value, status, order_no,
                           create_time, last_modify_time, del_flag, version)
VALUES
('dict_status_enabled', 'dict_status', 'enabled', '启用', '1', 1, 1, now(), now(), 0, 0),
('dict_status_disabled', 'dict_status', 'disabled', '禁用', '0', 1, 2, now(), now(), 0, 0),
('dict_gender_male', 'dict_gender', 'male', '男', 'male', 1, 1, now(), now(), 0, 0),
('dict_gender_female', 'dict_gender', 'female', '女', 'female', 1, 2, now(), now(), 0, 0),
('dict_gender_unknown', 'dict_gender', 'unknown', '未知', 'unknown', 1, 3, now(), now(), 0, 0),
('dict_business_type_other', 'dict_business_type', 'other', '其它', '0', 1, 0, now(), now(), 0, 0),
('dict_business_type_insert', 'dict_business_type', 'insert', '新增', '1', 1, 1, now(), now(), 0, 0),
('dict_business_type_update', 'dict_business_type', 'update', '修改', '2', 1, 2, now(), now(), 0, 0),
('dict_business_type_delete', 'dict_business_type', 'delete', '删除', '3', 1, 3, now(), now(), 0, 0),
('dict_business_type_api', 'dict_business_type', 'api', '调用API', '4', 1, 4, now(), now(), 0, 0),
('dict_business_type_export', 'dict_business_type', 'export', '导出', '5', 1, 5, now(), now(), 0, 0),
('dict_business_type_import', 'dict_business_type', 'import', '导入', '6', 1, 6, now(), now(), 0, 0),
('dict_business_type_query', 'dict_business_type', 'query', '查询', '7', 1, 7, now(), now(), 0, 0),
('dict_business_type_login', 'dict_business_type', 'login', '登录', '8', 1, 8, now(), now(), 0, 0),
('dict_business_type_logout', 'dict_business_type', 'logout', '登出', '9', 1, 9, now(), now(), 0, 0),
('dict_operator_type_other', 'dict_operator_type', 'other', '其它', '0', 1, 0, now(), now(), 0, 0),
('dict_operator_type_admin', 'dict_operator_type', 'admin', '后台用户', '1', 1, 1, now(), now(), 0, 0),
('dict_operator_type_mobile', 'dict_operator_type', 'mobile', '移动端用户', '2', 1, 2, now(), now(), 0, 0),
('dict_version_type_feature', 'dict_version_type', 'feature', '功能版本', 'feature', 1, 1, now(), now(), 0, 0),
('dict_version_type_fix', 'dict_version_type', 'fix', '修复版本', 'fix', 1, 2, now(), now(), 0, 0),
('dict_sms_supplier_alibaba', 'dict_sms_supplier_type', 'alibaba', '阿里云', 'alibaba', 1, 1, now(), now(), 0, 0),
('dict_sms_supplier_tencent', 'dict_sms_supplier_type', 'tencent', '腾讯云', 'tencent', 1, 2, now(), now(), 0, 0),
('dict_sms_supplier_huawei', 'dict_sms_supplier_type', 'huawei', '华为云', 'huawei', 1, 3, now(), now(), 0, 0),
('dict_sms_supplier_jdcloud', 'dict_sms_supplier_type', 'jdcloud', '京东云', 'jdcloud', 1, 4, now(), now(), 0, 0),
('dict_sms_supplier_yunpian', 'dict_sms_supplier_type', 'yunpian', '云片', 'yunpian', 1, 5, now(), now(), 0, 0),
('dict_sms_supplier_cloopen', 'dict_sms_supplier_type', 'cloopen', '容联云', 'cloopen', 1, 6, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    type_id = EXCLUDED.type_id,
    item_code = EXCLUDED.item_code,
    item_text = EXCLUDED.item_text,
    item_value = EXCLUDED.item_value,
    status = EXCLUDED.status,
    order_no = EXCLUDED.order_no,
    last_modify_time = now();
