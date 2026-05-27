-- P2 操作日志可靠性补丁
-- 为 Redis Stream 操作日志消费增加数据库幂等字段。

ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);
COMMENT ON COLUMN sys_oper_log.event_id IS '外部事件幂等ID';
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_oper_log_event_id
    ON sys_oper_log (event_id)
    WHERE event_id IS NOT NULL AND event_id <> '';

-- 如已存在季度分表，需要对每个历史/当前分表执行同类补丁。
-- 新建分表使用 CREATE TABLE ... LIKE sys_oper_log INCLUDING ALL 时会继承该字段与索引。
-- 示例：
-- ALTER TABLE sys_oper_log_2026_q1 ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);
-- CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_oper_log_2026_q1_event_id
--     ON sys_oper_log_2026_q1 (event_id)
--     WHERE event_id IS NOT NULL AND event_id <> '';
