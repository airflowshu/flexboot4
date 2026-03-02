-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    title VARCHAR(50) DEFAULT '',
    business_type INT DEFAULT 0,
    method VARCHAR(100) DEFAULT '',
    request_method VARCHAR(10) DEFAULT '',
    operator_type INT DEFAULT 0,
    oper_name VARCHAR(50) DEFAULT '',
    oper_user_id VARCHAR(50) DEFAULT '',
    dept_name VARCHAR(50) DEFAULT '',
    oper_url VARCHAR(255) DEFAULT '',
    oper_ip VARCHAR(128) DEFAULT '',
    oper_location VARCHAR(255) DEFAULT '',
    oper_param JSONB,
    json_result JSONB,
    status INT DEFAULT 0,
    error_msg TEXT,
    oper_time TIMESTAMP,
    cost_time BIGINT DEFAULT 0,
    ext_params JSONB,
    create_by VARCHAR(32) DEFAULT '',
    create_time TIMESTAMP,
    last_modify_by VARCHAR(32) DEFAULT '',
    last_modify_time TIMESTAMP,
    del_flag INT DEFAULT 0,
    version BIGINT DEFAULT 0,
    remark VARCHAR(500) DEFAULT ''
);

COMMENT ON TABLE sys_oper_log IS '操作日志记录';
COMMENT ON COLUMN sys_oper_log.title IS '模块标题';
COMMENT ON COLUMN sys_oper_log.business_type IS '业务类型';
COMMENT ON COLUMN sys_oper_log.method IS '方法名称';
COMMENT ON COLUMN sys_oper_log.request_method IS '请求方式';
COMMENT ON COLUMN sys_oper_log.operator_type IS '操作类别';
COMMENT ON COLUMN sys_oper_log.oper_name IS '操作人员';
COMMENT ON COLUMN sys_oper_log.oper_user_id IS '操作人员id';
COMMENT ON COLUMN sys_oper_log.dept_name IS '部门名称';
COMMENT ON COLUMN sys_oper_log.oper_url IS '请求URL';
COMMENT ON COLUMN sys_oper_log.oper_ip IS '主机地址';
COMMENT ON COLUMN sys_oper_log.oper_location IS '操作地点';
COMMENT ON COLUMN sys_oper_log.oper_param IS '请求参数';
COMMENT ON COLUMN sys_oper_log.json_result IS '返回参数';
COMMENT ON COLUMN sys_oper_log.status IS '操作状态（0正常 1异常）';
COMMENT ON COLUMN sys_oper_log.error_msg IS '错误消息';
COMMENT ON COLUMN sys_oper_log.oper_time IS '操作时间';
COMMENT ON COLUMN sys_oper_log.cost_time IS '消耗时间';

CREATE INDEX idx_sys_oper_log_bt ON sys_oper_log (business_type, status, create_time);
CREATE INDEX idx_sys_oper_log_ot ON sys_oper_log (oper_time);

-- 示例：2026年Q1分表
CREATE TABLE IF NOT EXISTS sys_oper_log_2026_q1 (LIKE sys_oper_log INCLUDING ALL);
