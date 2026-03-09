# flexboot4-cms-starter

CMS 内容管理扩展模块，提供：

- 栏目（分级）管理
- 文章管理（草稿 -> 提审 -> 审核通过/驳回 -> 发布）
- 文章附件管理（关联 `sys_file`，业务类型 `cms`）
- 标签管理与文章标签关系管理
- CMS 专用文件上传接口（默认公共桶 `flexboot4-public`）

## SQL

- 表结构脚本：`docs/sql/cms_pg.sql`
- 菜单初始化脚本：`docs/sql/cms_menu_pg.sql`

## API 路由前缀

- `/api/admin/cms/category`
- `/api/admin/cms/article`
- `/api/admin/cms/article-file`
- `/api/admin/cms/tag`
- `/api/admin/cms/article-tag`
- `/api/admin/cms/file`

