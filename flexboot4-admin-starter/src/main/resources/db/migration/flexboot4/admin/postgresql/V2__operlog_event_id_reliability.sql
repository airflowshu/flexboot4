-- FlexBoot4 Admin P2 operation log reliability patch (PostgreSQL)

ALTER TABLE sys_oper_log_2026_q2 ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);

COMMENT ON COLUMN sys_oper_log_2026_q2.event_id IS '外部事件幂等ID';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_oper_log_event_id
    ON sys_oper_log_2026_q2 (event_id)
    WHERE event_id IS NOT NULL AND event_id <> '';
