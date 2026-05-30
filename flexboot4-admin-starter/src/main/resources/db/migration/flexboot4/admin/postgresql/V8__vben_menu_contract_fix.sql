-- FlexBoot4 development data patch for the vben 5.7 backend route contract.
-- Safe to run multiple times on PostgreSQL.
--
-- Scope:
-- 1) Rename legacy sys_menu columns to vben-compatible names.
-- 2) Normalize root parent_id, layout component names and button nodes.
-- 3) Normalize CRUD permission code actions.
-- 4) Add or normalize critical Admin permission nodes used by the backend.

BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_menu' AND column_name = 'hide_menu'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_menu' AND column_name = 'hide_in_menu'
    ) THEN
        ALTER TABLE sys_menu RENAME COLUMN hide_menu TO hide_in_menu;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_menu' AND column_name = 'hide_breadcrumb'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_menu' AND column_name = 'hide_in_breadcrumb'
    ) THEN
        ALTER TABLE sys_menu RENAME COLUMN hide_breadcrumb TO hide_in_breadcrumb;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_menu' AND column_name = 'hide_tab'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'sys_menu' AND column_name = 'hide_in_tab'
    ) THEN
        ALTER TABLE sys_menu RENAME COLUMN hide_tab TO hide_in_tab;
    END IF;
END $$;

-- vben dynamic route roots use NULL parent_id.
UPDATE sys_menu
SET parent_id = NULL,
    last_modify_time = NOW()
WHERE parent_id = '0';

-- vben 5.7 layoutMap uses BasicLayout, not legacy LAYOUT.
UPDATE sys_menu
SET component = 'BasicLayout',
    last_modify_time = NOW()
WHERE component = 'LAYOUT';

-- Button nodes must not leak into the backend route tree.
UPDATE sys_menu
SET path = '',
    component = '',
    hide_in_menu = TRUE,
    keep_alive = FALSE,
    last_modify_time = NOW()
WHERE type = 'button'
  AND (COALESCE(path, '') <> ''
       OR COALESCE(component, '') <> ''
       OR COALESCE(hide_in_menu, FALSE) = FALSE
       OR COALESCE(keep_alive, TRUE) = TRUE);

-- Frontend/backend permission action names are standardized on add/edit/delete/list/export/import.
UPDATE sys_menu
SET auth_code = regexp_replace(auth_code, ':create$', ':add'),
    last_modify_time = NOW()
WHERE auth_code ~ ':create$';

UPDATE sys_menu
SET auth_code = regexp_replace(auth_code, ':update$', ':edit'),
    last_modify_time = NOW()
WHERE auth_code ~ ':update$';

UPDATE sys_menu
SET auth_code = regexp_replace(auth_code, ':remove$', ':delete'),
    last_modify_time = NOW()
WHERE auth_code ~ ':remove$';

-- Critical custom endpoints under /api/admin/**.
UPDATE sys_menu
SET auth_code = 'sys:oper:log:list',
    last_modify_time = NOW()
WHERE path IN ('/devops/log', '/system/oper-log', '/oper-log')
  AND COALESCE(auth_code, '') <> 'sys:oper:log:list';

UPDATE sys_menu
SET auth_code = 'sys:monitor:stats',
    last_modify_time = NOW()
WHERE path IN ('/devops/monitor', '/system/monitor', '/monitor')
  AND COALESCE(auth_code, '') <> 'sys:monitor:stats';

-- Reset password button under user menu.
WITH user_menu AS (
    SELECT id
    FROM sys_menu
    WHERE path = '/system/user'
       OR auth_code = 'sys:user:list'
       OR name IN ('SystemUser', 'SysUser')
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
    TRUE,
    FALSE,
    'button',
    'sys:user:reset-password',
    1,
    NOW(),
    NOW(),
    0,
    0
FROM user_menu
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE auth_code = 'sys:user:reset-password'
);

UPDATE sys_menu
SET auth_code = 'sys:user:reset-password',
    type = 'button',
    path = '',
    component = '',
    hide_in_menu = TRUE,
    keep_alive = FALSE,
    last_modify_time = NOW()
WHERE name = 'SystemUserResetPassword'
  AND (COALESCE(auth_code, '') <> 'sys:user:reset-password'
       OR COALESCE(type, '') <> 'button'
       OR COALESCE(path, '') <> ''
       OR COALESCE(component, '') <> ''
       OR COALESCE(hide_in_menu, FALSE) = FALSE);

COMMIT;
