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
COMMENT ON COLUMN sys_user_mfa.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_mfa.type IS 'MFA类型：TOTP';
COMMENT ON COLUMN sys_user_mfa.secret_ciphertext IS '加密后的TOTP密钥';
COMMENT ON COLUMN sys_user_mfa.device_name IS '设备名称';
COMMENT ON COLUMN sys_user_mfa.enabled IS '是否启用';
COMMENT ON COLUMN sys_user_mfa.confirmed_at IS '确认绑定时间';
COMMENT ON COLUMN sys_user_mfa.last_used_at IS '最近验证时间';

CREATE INDEX IF NOT EXISTS idx_sys_user_mfa_user_type_alive
    ON sys_user_mfa (user_id, type)
    WHERE del_flag = 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_mfa_enabled_totp
    ON sys_user_mfa (user_id, type)
    WHERE del_flag = 0
      AND enabled = TRUE
      AND type = 'TOTP';
