-- CMS 菜单初始化（示例）

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, type, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_root', '0', '/cms', 'Cms', 'LAYOUT', '内容管理', 'ant-design:book-outlined', 95,
        false, false, 'catalog', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_category', 'cms_menu_root', '/cms/category', 'CmsCategory', '/cms/category/index',
        '栏目管理', 'ant-design:folder-open-outlined', 1,
        false, true, 'menu', 'cms:category:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article', 'cms_menu_root', '/cms/article', 'CmsArticle', '/cms/article/index',
        '文章管理', 'ant-design:file-text-outlined', 2,
        false, true, 'menu', 'cms:article:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_tag', 'cms_menu_root', '/cms/tag', 'CmsTag', '/cms/tag/index',
        '标签管理', 'ant-design:tags-outlined', 3,
        false, true, 'menu', 'cms:tag:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article_file', 'cms_menu_root', '/cms/article-file', 'CmsArticleFile', '/cms/article-file/index',
        '文章附件', 'ant-design:paper-clip-outlined', 4,
        false, true, 'menu', 'cms:article:file:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article_review_btn', 'cms_menu_article', '', 'CmsArticleReview', '',
        '文章审核', '', 99,
        true, false, 'button', 'cms:article:review', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

