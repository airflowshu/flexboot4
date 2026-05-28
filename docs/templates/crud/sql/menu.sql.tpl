-- {{entity}} vben menu and CRUD button permissions.

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('{{menu.idPrefix}}', '{{menu.parentId}}', '{{menu.path}}', '{{menu.name}}', '{{menu.component}}',
        '{{menu.title}}', '{{menu.icon}}', {{menu.orderNo}},
        FALSE, TRUE, 'menu', '{{permissionPrefix}}:list', 1,
        NOW(), NOW(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    path = EXCLUDED.path,
    name = EXCLUDED.name,
    component = EXCLUDED.component,
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    type = EXCLUDED.type,
    auth_code = EXCLUDED.auth_code,
    last_modify_time = NOW();

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES
('{{menu.idPrefix}}_add_btn', '{{menu.idPrefix}}', '', '{{menu.name}}Add', '', 'common.add', '', 10, TRUE, FALSE, 'button', '{{permissionPrefix}}:add', 1, NOW(), NOW(), 0, 0),
('{{menu.idPrefix}}_edit_btn', '{{menu.idPrefix}}', '', '{{menu.name}}Edit', '', 'common.edit', '', 20, TRUE, FALSE, 'button', '{{permissionPrefix}}:edit', 1, NOW(), NOW(), 0, 0),
('{{menu.idPrefix}}_delete_btn', '{{menu.idPrefix}}', '', '{{menu.name}}Delete', '', 'common.delete', '', 30, TRUE, FALSE, 'button', '{{permissionPrefix}}:delete', 1, NOW(), NOW(), 0, 0),
('{{menu.idPrefix}}_export_btn', '{{menu.idPrefix}}', '', '{{menu.name}}Export', '', 'common.export', '', 40, TRUE, FALSE, 'button', '{{permissionPrefix}}:export', 1, NOW(), NOW(), 0, 0),
('{{menu.idPrefix}}_import_btn', '{{menu.idPrefix}}', '', '{{menu.name}}Import', '', 'common.import', '', 50, TRUE, FALSE, 'button', '{{permissionPrefix}}:import', 1, NOW(), NOW(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    path = '',
    component = '',
    hide_in_menu = TRUE,
    keep_alive = FALSE,
    type = 'button',
    auth_code = EXCLUDED.auth_code,
    last_modify_time = NOW();
