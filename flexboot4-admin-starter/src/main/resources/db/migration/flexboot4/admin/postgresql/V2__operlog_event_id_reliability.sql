-- FlexBoot4 Admin operation log reliability patch (PostgreSQL)
-- Adds an idempotency field to the base operation-log table and any already
-- existing quarterly tables. Future quarterly tables inherit this from
-- sys_oper_log via CREATE TABLE ... LIKE sys_oper_log INCLUDING ALL.

ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);
COMMENT ON COLUMN sys_oper_log.event_id IS '外部事件幂等ID';
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_oper_log_event_id
    ON sys_oper_log (event_id)
    WHERE event_id IS NOT NULL AND event_id <> '';

DO $$
DECLARE
    log_table record;
    index_name text;
BEGIN
    FOR log_table IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = current_schema()
          AND tablename ~ '^sys_oper_log_[0-9]{4}_q[1-4]$'
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS event_id VARCHAR(128)', log_table.tablename);
        EXECUTE format('COMMENT ON COLUMN %I.event_id IS %L', log_table.tablename, '外部事件幂等ID');

        index_name := left('uk_' || log_table.tablename || '_event_id', 63);
        EXECUTE format(
            'CREATE UNIQUE INDEX IF NOT EXISTS %I ON %I (event_id) WHERE event_id IS NOT NULL AND event_id <> %L',
            index_name,
            log_table.tablename,
            ''
        );
    END LOOP;
END $$;
