# flexboot4-cms-starter

`flexboot4-cms-starter` 提供 FlexBoot4 的 CMS 内容管理与静态站点发布能力，当前包含：

- 栏目管理（树形栏目）
- 文章管理（草稿 / 提审 / 审核 / 发布）
- 文章附件与标签管理
- CMS 文件上传接口
- **模板文件管理 + iframe 在线预览 + 直接回写 + 静态发布 ZIP（v1）**

## v1 模板管理能力

### 后端接口

- `GET /api/admin/cms/template/tree`：递归读取模板 HTML 文件树
- `GET /api/admin/cms/template/file?path=...`：读取模板源码与预览 HTML
- `PUT /api/admin/cms/template/file`：直接回写 HTML 文件
- `POST /api/admin/cms/template/publish`：复制当前模板目录并生成 ZIP 包
- `GET /api/admin/cms/template/publish/history`：分页查询发布记录

### 前端页面

- 页面路径：`/cms/template/index`
- 页面能力：
  - 左侧模板目录树
  - 中间源码编辑区
  - 右侧 iframe 实时预览
  - 顶部保存 / 重载 / 发布 ZIP
  - 底部发布记录与结果查看

### 模板目录与发布目录配置

```yaml
cms:
  template:
    root-dir: ${CMS_TEMPLATE_ROOT_DIR:${user.dir}/webapp/html/web}
    asset-base-url: ${CMS_TEMPLATE_ASSET_BASE_URL:http://localhost:8080}
    publish-dir: ${CMS_TEMPLATE_PUBLISH_DIR:${cms.render.output-dir}/site-published}
```

说明：

- `root-dir`：模板 HTML 根目录，只允许访问该目录下的 `.html` 文件
- `asset-base-url`：预览时用于重写 `/template/1/default`、`/public/images`、`/uploads` 等资源地址
- `publish-dir`：发布时复制模板树到该目录下的 `current` 目录，ZIP 包输出到同级 `site-packages`

### 预览资源重写规则

预览接口会将模板中的资源地址统一重写到 `cms.template.asset-base-url`：

- `/template/1/default/...` → `{assetBaseUrl}/template/1/default/...`
- `/public/images/...` → `{assetBaseUrl}/public/images/...`
- `http://localhost:8080/uploads/...` → `{assetBaseUrl}/uploads/...`

### 发布产物

发布成功后会生成：

- 静态目录：`${cms.template.publish-dir}/current`
- ZIP 包目录：`${cms.render.output-dir}/site-packages`
- 发布记录表：`cms_template_publish_record`

## SQL

- 表结构脚本：`docs/sql/cms_pg.sql`
- 菜单初始化脚本：`docs/sql/cms_menu_init_pg.sql`
- 兼容菜单脚本别名：`docs/sql/cms_menu_pg.sql`

## API 路由前缀

- `/api/admin/cms/category`
- `/api/admin/cms/article`
- `/api/admin/cms/article-file`
- `/api/admin/cms/tag`
- `/api/admin/cms/article-tag`
- `/api/admin/cms/file`
- `/api/admin/cms/template`
