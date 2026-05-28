-- FlexBoot4 Admin file storage patch (PostgreSQL)
-- 本地/MinIO 统一文件存储改造后，删除文件会同步删除实际存储对象。
-- 因此 file_hash 去重只允许复用未删除记录，不能复活已软删除记录。

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
