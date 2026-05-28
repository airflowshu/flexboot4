-- CMS 菜单初始化（示例）

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_root', null, '/cms', 'Cms', 'BasicLayout', 'cms.title', 'ant-design:book-outlined', 95,
        false, false, 'catalog', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_category', 'cms_menu_root', '/cms/category', 'CmsCategory', '/cms/category/index',
        'cms.category.title', 'ant-design:folder-open-outlined', 1,
        false, true, 'menu', 'cms:category:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article', 'cms_menu_root', '/cms/article', 'CmsArticle', '/cms/article/index',
        'cms.article.title', 'ant-design:file-text-outlined', 2,
        false, true, 'menu', 'cms:article:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_tag', 'cms_menu_root', '/cms/tag', 'CmsTag', '/cms/tag/index',
        'cms.tag.title', 'ant-design:tags-outlined', 3,
        false, true, 'menu', 'cms:tag:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article_file', 'cms_menu_root', '/cms/article-file', 'CmsArticleFile', '/cms/article-file/index',
        'cms.articleFile.title', 'ant-design:paper-clip-outlined', 4,
        false, true, 'menu', 'cms:article:file:list', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_template', 'cms_menu_root', '/cms/template', 'CmsTemplate', '/cms/template/index',
        'cms.template.title', 'ant-design:layout-outlined', 5,
        false, true, 'menu', 'cms:template:view', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article_review_btn', 'cms_menu_article', '', 'CmsArticleReview', '',
        'cms.article.review', '', 99,
        true, false, 'button', 'cms:article:review', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article_submit_btn', 'cms_menu_article', '', 'CmsArticleSubmit', '',
        'cms.article.submit', '', 96,
        true, false, 'button', 'cms:article:submit', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_article_preview_btn', 'cms_menu_article', '', 'CmsArticlePreview', '',
        'cms.article.preview', '', 97,
        true, false, 'button', 'cms:article:preview', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_file_upload_btn', 'cms_menu_article_file', '', 'CmsFileUpload', '',
        'cms.file.upload', '', 97,
        true, false, 'button', 'cms:file:upload', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_template_edit_btn', 'cms_menu_template', '', 'CmsTemplateEdit', '',
        'cms.template.save', '', 98,
        true, false, 'button', 'cms:template:edit', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, path, name, component, title, icon, order_no,
                      hide_in_menu, keep_alive, type, auth_code, status,
                      create_time, last_modify_time, del_flag, version)
VALUES ('cms_menu_template_publish_btn', 'cms_menu_template', '', 'CmsTemplatePublish', '',
        'cms.template.publish', '', 99,
        true, false, 'button', 'cms:template:publish', 1,
        now(), now(), 0, 0)
ON CONFLICT (id) DO NOTHING;
