-- FlexBoot4 Admin P0 permission patch (PostgreSQL)

UPDATE sys_menu
SET auth_code = 'sys:oper:log:list',
    last_modify_time = NOW()
WHERE path = '/devops/log'
  AND COALESCE(auth_code, '') <> 'sys:oper:log:list';

UPDATE sys_menu
SET auth_code = 'sys:monitor:stats',
    last_modify_time = NOW()
WHERE path = '/devops/monitor'
  AND COALESCE(auth_code, '') <> 'sys:monitor:stats';

WITH user_menu AS (
    SELECT id
    FROM sys_menu
    WHERE path = '/system/user'
    ORDER BY last_modify_time DESC NULLS LAST, create_time DESC NULLS LAST
    LIMIT 1
)
INSERT INTO sys_menu (
    id, parent_id, path, name, component, title, icon, order_no,
    hide_in_menu, keep_alive, type, auth_code, status,
    create_time, last_modify_time, del_flag, version
)
SELECT
    'system_user_reset_password_btn',
    user_menu.id,
    '',
    'SystemUserResetPassword',
    '',
    'common.resetPassword',
    '',
    91,
    true,
    false,
    'button',
    'sys:user:reset-password',
    1,
    NOW(),
    NOW(),
    0,
    0
FROM user_menu
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_menu
    WHERE auth_code = 'sys:user:reset-password'
);

UPDATE sys_menu
SET auth_code = 'sys:user:reset-password',
    last_modify_time = NOW()
WHERE name = 'SystemUserResetPassword'
  AND COALESCE(auth_code, '') <> 'sys:user:reset-password';

