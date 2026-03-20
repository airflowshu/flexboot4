# CMS 模块快速开始指南
## 3. API 使用示例

### 3.1 创建栏目

```bash
curl -X POST http://localhost:8080/api/admin/cms/category \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "categoryName": "技术博客",
    "categoryCode": "tech-blog",
    "description": "技术相关文章栏目",
    "sortOrder": 1,
    "status": 1
  }'
```

### 3.2 创建标签

```bash
curl -X POST http://localhost:8080/api/admin/cms/tag \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "tagName": "Java",
    "tagColor": "#FF5733"
  }'
```

### 3.3 上传 CMS 文件

```bash
curl -X POST http://localhost:8080/api/admin/cms/file/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/image.jpg" \
  -F "tenantId=1"
```

返回示例：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": "1234567890",
    "fileName": "image.jpg",
    "fileSize": 102400,
    "location": {
      "bucket": "flexboot4-public",
      "objectKey": "cms/2026/03/xxx.jpg",
      "endpoint": "http://minio:9000"
    }
  }
}
```

### 3.4 创建文章草稿

```bash
curl -X POST http://localhost:8080/api/admin/cms/article \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "如何使用 FlexBoot4 CMS",
    "categoryId": "栏目ID",
    "author": "张三",
    "coverFileId": "封面图文件ID",
    "summary": "本文介绍如何使用 FlexBoot4 CMS 模块",
    "content": "<p>文章内容...</p>",
    "status": "DRAFT",
    "sortOrder": 0
  }'
```

### 3.5 提交文章审核

```bash
curl -X POST http://localhost:8080/api/admin/cms/article/{articleId}/submit \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3.6 审核通过（需要 cms:article:review 权限）

```bash
curl -X POST http://localhost:8080/api/admin/cms/article/{articleId}/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "reviewComment": "内容质量很好，审核通过"
  }'
```

### 3.7 驳回文章（需要 cms:article:review 权限）

```bash
curl -X POST http://localhost:8080/api/admin/cms/article/{articleId}/reject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "reviewComment": "标题需要修改，请重新提交"
  }'
```

### 3.8 增加浏览量

```bash
curl -X POST http://localhost:8080/api/admin/cms/article/{articleId}/view \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3.9 分页查询文章

```bash
curl -X POST http://localhost:8080/api/admin/cms/article/page \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "pageNumber": 1,
    "pageSize": 10,
    "logic": "AND",
    "items": [
      { "field": "status", "op": "eq", "val": "PUBLISHED" }
    ],
    "orders": [
      { "column": "publishTime", "asc": false }
    ]
  }'
```

### 3.10 关联文章附件

```bash
curl -X POST http://localhost:8080/api/admin/cms/article-file \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "articleId": "文章ID",
    "fileId": "文件ID",
    "sortOrder": 1
  }'
```

### 3.11 关联文章标签

```bash
curl -X POST http://localhost:8080/api/admin/cms/article-tag \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "articleId": "文章ID",
    "tagId": "标签ID"
  }'
```

## 4. 文章状态流转

```
DRAFT (草稿)
    ↓ submitForReview()
PENDING (待审核)
    ↓ approveArticle()
PUBLISHED (已发布)

PENDING (待审核)
    ↓ rejectArticle()
REJECTED (已驳回)
    ↓ submitForReview()
PENDING (待审核)
```

## 5. 权限配置

在 `sys_menu` 表中已经包含以下权限码：

- `cms:category:list` - 栏目查询
- `cms:category:create` - 栏目创建（BaseController 自动）
- `cms:category:update` - 栏目更新（BaseController 自动）
- `cms:category:delete` - 栏目删除（BaseController 自动）
- `cms:article:list` - 文章查询
- `cms:article:create` - 文章创建
- `cms:article:update` - 文章更新
- `cms:article:delete` - 文章删除
- `cms:article:review` - 文章审核（需要手动分配给管理员角色）
- `cms:tag:list` - 标签管理
- `cms:article:file:list` - 文章附件管理

### 为管理员角色分配审核权限

```sql
-- 查找管理员角色ID和审核按钮菜单ID
SELECT id FROM sys_role WHERE role_value = 'admin';
SELECT id FROM sys_menu WHERE auth_code = 'cms:article:review';

-- 分配权限
INSERT INTO sys_role_menu (id, role_id, menu_id, create_time, last_modify_time)
VALUES (
  'role_menu_cms_review',
  '管理员角色ID',
  'cms_menu_article_review_btn',
  now(),
  now()
);
```

## 6. 前端路由建议

前端项目可以按照以下结构组织：

```
/cms
  /category      # 栏目管理（树形结构）
  /article       # 文章管理
    /list        # 文章列表（带筛选：状态、栏目、标签）
    /create      # 创建文章
    /edit/:id    # 编辑文章
    /preview/:id # 预览文章
  /tag           # 标签管理
  /article-file  # 附件管理
```

## 7. 常见问题

### Q1: 如何确保文件上传到公共桶？

A: 使用 CMS 专用上传接口 `/api/admin/cms/file/upload`，它会自动设置 `bizType=cms` 和 `bucketName=flexboot4-public`。

### Q2: 普通用户可以审核文章吗？

A: 不可以。审核接口使用了 `@RequirePermission("cms:article:review")` 注解，只有拥有该权限的管理员才能审核。

### Q3: 如何关联文章和标签？

A: 先创建标签（在标签管理中），然后通过 `cms_article_tag` 接口关联文章和标签。

### Q4: 文章内容中的图片如何处理？

A: 富文本编辑器上传的图片通过 `/api/admin/cms/file/upload` 上传，返回的 URL 直接插入到 `content` 字段中。独立的附件通过 `cms_article_file` 表管理。

### Q5: 如何实现栏目层级？

A: 创建子栏目时，设置 `parentId` 为父栏目的 ID。根栏目的 `parentId` 为 `null`。


## 9. 开发建议

1. **标签预创建**：在发布文章前，先在标签管理中创建所需的标签
2. **封面图尺寸**：建议统一封面图尺寸（如 800x450）以保持一致性
3. **文章摘要**：建议限制在 200 字以内
4. **审核意见**：审核时尽量填写具体的审核意见，方便作者修改
---

**版本**：v1.0  
**更新日期**：2026-03-06

