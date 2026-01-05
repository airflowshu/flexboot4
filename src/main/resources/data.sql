-- 1. Demos (Root)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (100, 0, '/demos', 'Demos', NULL, '/demos/access', 'demos.title', 'ic:baseline-view-in-ar', 1000, true, 0, 1);

-- 2. AccessDemos (Child of Demos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (101, 100, '/demos/access', 'AccessDemos', NULL, '/demos/access/page-control', 'demos.access.backendPermissions', 'mdi:cloud-key-outline', 0, true, 0, 1);

-- 3. AccessPageControlDemo (Child of AccessDemos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (102, 101, '/demos/access/page-control', 'AccessPageControlDemo', '/demos/access/index', NULL, 'demos.access.pageAccess', 'mdi:page-previous-outline', 1, true, 1, 1);

-- 4. AccessButtonControlDemo (Child of AccessDemos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (103, 101, '/demos/access/button-control', 'AccessButtonControlDemo', '/demos/access/button-control', NULL, 'demos.access.buttonControl', 'mdi:button-cursor', 2, true, 1, 1);

-- 5. AccessMenuVisible403Demo (Child of AccessDemos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, menu_visible_with_forbidden, authority, type, status)
VALUES (104, 101, '/demos/access/menu-visible-403', 'AccessMenuVisible403Demo', '/demos/access/menu-visible-403', NULL, 'demos.access.menuVisible403', 'mdi:button-cursor', 3, true, true, 'no-body', 1, 1);

-- 6. AccessSuperVisibleDemo (Child of AccessDemos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (105, 101, '/demos/access/super-visible', 'AccessSuperVisibleDemo', '/demos/access/super-visible', NULL, 'demos.access.superVisible', 'mdi:button-cursor', 4, true, 1, 1);

-- 7. AccessUserVisibleDemo (Child of AccessDemos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (106, 101, '/demos/access/user-visible', 'AccessUserVisibleDemo', '/demos/access/user-visible', NULL, 'demos.access.userVisible', 'mdi:button-cursor', 5, true, 1, 1);

-- 8. AccessAdminVisibleDemo (Child of AccessDemos)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (107, 101, '/demos/access/admin-visible', 'AccessAdminVisibleDemo', '/demos/access/admin-visible', NULL, 'demos.access.adminVisible', 'mdi:button-cursor', 6, true, 1, 1);

-- 9. Workspace
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, affix_tab, type, status)
VALUES (108, 0, '/workspace', 'Workspace', '/dashboard/workspace/index', NULL, 'page.dashboard.workspace', 'carbon:workspace', 0, true, true, 1, 1);

-- 10. System (Catalog)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, badge, badge_type, badge_variants, type, status)
VALUES (109, 0, '/system', 'System', NULL, NULL, 'system.title', 'carbon:settings', 9997, true, 'new', 'normal', 'primary', 0, 1);

-- 11. SystemMenu (Child of System)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (110, 109, '/system/menu', 'SystemMenu', '/system/menu/list', NULL, 'system.menu.title', 'carbon:menu', 0, true, 'System:Menu:List', 1, 1);

-- 12. SystemMenu Buttons
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (111, 110, NULL, 'SystemMenuCreate', NULL, NULL, 'common.create', NULL, 0, false, 'System:Menu:Create', 2, 1);

INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (112, 110, NULL, 'SystemMenuEdit', NULL, NULL, 'common.edit', NULL, 0, false, 'System:Menu:Edit', 2, 1);

INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (113, 110, NULL, 'SystemMenuDelete', NULL, NULL, 'common.delete', NULL, 0, false, 'System:Menu:Delete', 2, 1);

-- 13. SystemDept (Child of System)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (114, 109, '/system/dept', 'SystemDept', '/system/dept/list', NULL, 'system.dept.title', 'carbon:container-services', 0, true, 'System:Dept:List', 1, 1);

-- 14. SystemDept Buttons
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (115, 114, NULL, 'SystemDeptCreate', NULL, NULL, 'common.create', NULL, 0, false, 'System:Dept:Create', 2, 1);

INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (116, 114, NULL, 'SystemDeptEdit', NULL, NULL, 'common.edit', NULL, 0, false, 'System:Dept:Edit', 2, 1);

INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, permission, type, status)
VALUES (117, 114, NULL, 'SystemDeptDelete', NULL, NULL, 'common.delete', NULL, 0, false, 'System:Dept:Delete', 2, 1);

-- 15. Project (Catalog)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, badge_type, type, status)
VALUES (118, 0, '/vben-admin', 'Project', NULL, NULL, 'demos.vben.title', 'carbon:data-center', 9998, true, 'dot', 0, 1);

-- 16. VbenDocument (Child of Project)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, iframe_src, type, status)
VALUES (119, 118, '/vben-admin/document', 'VbenDocument', 'IFrameView', NULL, 'demos.vben.document', 'carbon:book', 0, true, 'https://doc.vben.pro', 3, 1);

-- 17. VbenGithub (Child of Project)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, link, type, status)
VALUES (120, 118, '/vben-admin/github', 'VbenGithub', 'IFrameView', NULL, 'Github', 'carbon:logo-github', 0, true, 'https://github.com/vbenjs/vue-vben-admin', 4, 1);

-- 18. VbenAntdv (Child of Project)
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, link, badge_type, type, status)
VALUES (121, 118, '/vben-admin/antdv', 'VbenAntdv', 'IFrameView', NULL, 'demos.vben.antdv', 'carbon:hexagon-vertical-solid', 0, true, 'https://ant.vben.pro', 'dot', 4, 0);

-- 19. About
INSERT INTO sys_menu (id, parent_id, path, name, component, redirect, title, icon, order_no, keep_alive, type, status)
VALUES (122, 0, '/about', 'About', '_core/about/index', NULL, 'demos.vben.about', 'lucide:copyright', 9999, true, 1, 1);
