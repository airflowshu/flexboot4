-- ============================================================
-- sys_menu 初始化数据：短信管理菜单
-- ============================================================

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('sms_menu_root', null, '/sms', 'Sms', 'BasicLayout', 'sms.title', 'ant-design:message-outlined', 90,
        false, false, 'catalog', 1, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = NULL,
    path = EXCLUDED.path,
    name = EXCLUDED.name,
    component = EXCLUDED.component,
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    order_no = EXCLUDED.order_no,
    hide_in_menu = EXCLUDED.hide_in_menu,
    keep_alive = EXCLUDED.keep_alive,
    auth_code = NULL,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    last_modify_time = NOW();

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('sms_menu_config', 'sms_menu_root', '/sms/config', 'SmsConfig',
        '/sms/config/index', 'sms.config.title', 'ant-design:setting-outlined', 1,
        false, true, 'menu', 'sms4j:config:list', 1, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    path = EXCLUDED.path,
    name = EXCLUDED.name,
    component = EXCLUDED.component,
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    order_no = EXCLUDED.order_no,
    hide_in_menu = EXCLUDED.hide_in_menu,
    keep_alive = EXCLUDED.keep_alive,
    type = EXCLUDED.type,
    auth_code = EXCLUDED.auth_code,
    status = EXCLUDED.status,
    last_modify_time = NOW();

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES
('sms_menu_config_add_btn', 'sms_menu_config', '', 'SmsConfigAdd', '', 'common.add', '', 10, true, false, 'button', 'sms4j:config:add', 1, now(), now(), 0, 0),
('sms_menu_config_edit_btn', 'sms_menu_config', '', 'SmsConfigEdit', '', 'common.edit', '', 20, true, false, 'button', 'sms4j:config:edit', 1, now(), now(), 0, 0),
('sms_menu_config_delete_btn', 'sms_menu_config', '', 'SmsConfigDelete', '', 'common.delete', '', 30, true, false, 'button', 'sms4j:config:delete', 1, now(), now(), 0, 0),
('sms_menu_config_export_btn', 'sms_menu_config', '', 'SmsConfigExport', '', 'common.export', '', 40, true, false, 'button', 'sms4j:config:export', 1, now(), now(), 0, 0),
('sms_menu_config_import_btn', 'sms_menu_config', '', 'SmsConfigImport', '', 'common.import', '', 50, true, false, 'button', 'sms4j:config:import', 1, now(), now(), 0, 0),
('sms_menu_config_test_btn', 'sms_menu_config', '', 'SmsConfigTest', '', '测试', '', 60, true, false, 'button', 'sms4j:config:test', 1, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    path = '',
    name = EXCLUDED.name,
    component = '',
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    order_no = EXCLUDED.order_no,
    hide_in_menu = true,
    keep_alive = false,
    type = 'button',
    auth_code = EXCLUDED.auth_code,
    status = EXCLUDED.status,
    last_modify_time = NOW();
