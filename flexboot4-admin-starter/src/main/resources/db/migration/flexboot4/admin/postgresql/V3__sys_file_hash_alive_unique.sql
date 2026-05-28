-- FlexBoot4 Admin file storage patch (PostgreSQL)
-- File records are physically removed from storage on delete, so duplicate
-- detection must only reuse alive sys_file rows.

ALTER TABLE sys_file DROP CONSTRAINT IF EXISTS uk_sys_file_hash;
ALTER TABLE sys_file DROP CONSTRAINT IF EXISTS uk_sys_file_file_hash;
ALTER TABLE sys_file DROP CONSTRAINT IF EXISTS uk_file_hash;

DROP INDEX IF EXISTS uk_sys_file_hash;
DROP INDEX IF EXISTS uk_sys_file_file_hash;
DROP INDEX IF EXISTS uk_file_hash;
DROP INDEX IF EXISTS uk_sys_file_file_hash_alive;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_file_hash_alive
    ON sys_file (file_hash)
    WHERE del_flag = 0;
