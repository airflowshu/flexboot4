-- ============================================================
-- sys_menu 初始化数据：知识库菜单与按钮权限
-- ============================================================

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('kb_menu_root', null, '/kb', 'Kb', 'BasicLayout', '知识库', 'ant-design:book-outlined', 85,
        false, false, 'catalog', 1, now(), now(), 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = null,
    path = EXCLUDED.path,
    name = EXCLUDED.name,
    component = EXCLUDED.component,
    title = EXCLUDED.title,
    icon = EXCLUDED.icon,
    order_no = EXCLUDED.order_no,
    hide_in_menu = EXCLUDED.hide_in_menu,
    keep_alive = EXCLUDED.keep_alive,
    auth_code = null,
    type = EXCLUDED.type,
    status = EXCLUDED.status,
    last_modify_time = now();

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('kb_menu_manage', 'kb_menu_root', '/kb/manage', 'KbManage',
        '/kb/index', '知识库管理', 'ant-design:read-outlined', 1,
        false, true, 'menu', 'kb:manage:list', 1, now(), now(), 0, 0)
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
    last_modify_time = now();

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES
('kb_menu_manage_add_btn', 'kb_menu_manage', '', 'KbManageAdd', '', 'common.add', '', 10, true, false, 'button', 'kb:manage:add', 1, now(), now(), 0, 0),
('kb_menu_manage_edit_btn', 'kb_menu_manage', '', 'KbManageEdit', '', 'common.edit', '', 20, true, false, 'button', 'kb:manage:edit', 1, now(), now(), 0, 0),
('kb_menu_manage_delete_btn', 'kb_menu_manage', '', 'KbManageDelete', '', 'common.delete', '', 30, true, false, 'button', 'kb:manage:delete', 1, now(), now(), 0, 0),
('kb_menu_member_list_btn', 'kb_menu_manage', '', 'KbMemberList', '', '知识库成员列表', '', 40, true, false, 'button', 'kb:member:list', 1, now(), now(), 0, 0),
('kb_menu_member_add_btn', 'kb_menu_manage', '', 'KbMemberAdd', '', '添加知识库成员', '', 50, true, false, 'button', 'kb:member:add', 1, now(), now(), 0, 0),
('kb_menu_member_delete_btn', 'kb_menu_manage', '', 'KbMemberDelete', '', '移除知识库成员', '', 60, true, false, 'button', 'kb:member:delete', 1, now(), now(), 0, 0),
('kb_menu_index_run_btn', 'kb_menu_manage', '', 'KbIndexRun', '', '知识库索引', '', 70, true, false, 'button', 'kb:index:run', 1, now(), now(), 0, 0),
('kb_menu_file_list_btn', 'kb_menu_manage', '', 'KbFileList', '', '知识库文件列表', '', 80, true, false, 'button', 'kb:file:list', 1, now(), now(), 0, 0),
('kb_menu_file_add_btn', 'kb_menu_manage', '', 'KbFileAdd', '', '添加知识库文件', '', 90, true, false, 'button', 'kb:file:add', 1, now(), now(), 0, 0),
('kb_menu_file_edit_btn', 'kb_menu_manage', '', 'KbFileEdit', '', '编辑知识库文件', '', 100, true, false, 'button', 'kb:file:edit', 1, now(), now(), 0, 0),
('kb_menu_file_delete_btn', 'kb_menu_manage', '', 'KbFileDelete', '', '删除知识库文件', '', 110, true, false, 'button', 'kb:file:delete', 1, now(), now(), 0, 0)
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
    last_modify_time = now();
