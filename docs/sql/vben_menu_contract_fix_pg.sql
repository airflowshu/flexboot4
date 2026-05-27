-- Align existing development data with the vben 5.7 backend route contract.
-- Run this once before using the updated backend route adapter.

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

UPDATE sys_menu
SET parent_id = null,
    last_modify_time = now()
WHERE parent_id = '0';

UPDATE sys_menu
SET component = 'BasicLayout',
    last_modify_time = now()
WHERE component = 'LAYOUT';

UPDATE sys_menu
SET auth_code = regexp_replace(auth_code, ':create$', ':add'),
    last_modify_time = now()
WHERE auth_code ~ ':create$';

