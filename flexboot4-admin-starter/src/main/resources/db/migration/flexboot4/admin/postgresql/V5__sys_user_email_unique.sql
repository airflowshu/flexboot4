-- 备用邮箱唯一绑定约束。
-- 当前项目仍处于开发阶段，若存在重复邮箱，本脚本会中断并提示先清理数据。

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys_user
        WHERE del_flag = 0
          AND email IS NOT NULL
          AND btrim(email) <> ''
        GROUP BY lower(btrim(email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'sys_user.email has duplicate active values. Clean duplicate email data before enabling security email binding.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_email_alive
    ON sys_user (lower(btrim(email)))
    WHERE del_flag = 0
      AND email IS NOT NULL
      AND btrim(email) <> '';
