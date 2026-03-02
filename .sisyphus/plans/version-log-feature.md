# 版本日志功能实施计划

## 📋 功能概述

在 flexboot-admin 模块的运维管理(ops)中新增版本日志功能，用于记录系统版本更新内容，并提供前端接口分页展示。

## 🎯 需求确认

- **展示方式**：时间线展示（按发布日期倒序）
- **分类支持**：功能更新（新增、修复、优化）
- **菜单位置**：系统管理菜单下
- **权限控制**：管理员维护，用户可查看
- **数据库DDL**：提供DDL脚本
- **开发模式**：完全遵循 FlexBoot4 标准三层架构（参照 SysDictTypeController 模式）

---

## 📐 标准架构模式（参照 SysDictTypeController）

```
Controller (继承 BaseController)
    ↓
Service (继承 IExtendedService)
    ↓
ServiceImpl (继承 BaseServiceImpl，使用 @CacheConfig)
    ↓
Mapper (继承 BaseMapper)
    ↓
Entity (继承 BaseEntity)
```

**标准特点**：
- Controller 极度精简，只重写 `getEntityClass()`
- Service 层面向接口编程，继承 `IExtendedService<T>`
- ServiceImpl 使用 `@CacheConfig(cacheNames = "...")`
- Mapper 只需继承 `BaseMapper<T>`

---

## 1. 数据库层设计

### 1.1 新增枚举类型

**文件**: `flexboot4-admin/src/main/java/com/yunlbd/flexboot4/common/enums/VersionLogType.java`

```java
/**
 * 版本日志类型枚举
 */
public enum VersionLogType {
    FEATURE("feature", "新功能"),
    BUG_FIX("bug_fix", "缺陷修复"),
    OPTIMIZATION("optimization", "性能优化"),
    DOCUMENT("document", "文档更新");

    private final String code;
    private final String description;
}
```

### 1.2 新增版本日志实体

**文件**: `flexboot4-admin/src/main/java/com/yunlbd/flexboot4/entity/ops/SysVersionLog.java`

```java
/**
 * 版本日志实体类
 *
 * @author Wangts
 * @since 1.0.0
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_version_log")
@Schema(name = "SysVersionLog")
public class SysVersionLog extends BaseEntity {

    @Schema(title = "版本号")
    private String versionNo;

    @Schema(title = "发布日期")
    private LocalDateTime releaseDate;

    @Schema(title = "类型")
    private String type;

    @Schema(title = "标题")
    private String title;

    @Schema(title = "更新内容")
    private String content;

    @Schema(title = "状态")
    private Integer status; // 0-草稿, 1-已发布
}
```

### 1.3 创建DDL脚本

**文件**: `flexboot4-admin/src/main/resources/db/sys_version_log.sql`

```sql
-- 版本日志表
CREATE TABLE sys_version_log (
    id VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid(),
    version_no VARCHAR(50) NOT NULL,
    release_date TIMESTAMP NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status INTEGER DEFAULT 0,
    version BIGINT DEFAULT 0,
    del_flag INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modify_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100),
    last_modify_by VARCHAR(100),
    remark VARCHAR(500),
    CONSTRAINT uk_sys_version_log_version_no UNIQUE (version_no)
);

-- 索引
CREATE INDEX idx_sys_version_log_release_date ON sys_version_log(release_date DESC);
CREATE INDEX idx_sys_version_log_status ON sys_version_log(status);

-- 注释
COMMENT ON TABLE sys_version_log IS '系统版本日志';
COMMENT ON COLUMN sys_version_log.version_no IS '版本号';
COMMENT ON COLUMN sys_version_log.release_date IS '发布日期';
COMMENT ON COLUMN sys_version_log.type IS '类型:feature-新功能,bug_fix-缺陷修复,optimization-性能优化,document-文档更新';
COMMENT ON COLUMN sys_version_log.title IS '标题';
COMMENT ON COLUMN sys_version_log.content IS '更新内容';
COMMENT ON COLUMN sys_version_log.status IS '状态:0-草稿,1-已发布';
```

---

## 2. Mapper层（标准实现）

**文件**: `flexboot4-admin/src/main/java/com/yunlbd/flexboot4/mapper/SysVersionLogMapper.java`

```java
/**
 * 映射层
 *
 * @author Wangts
 * @since 1.0.0
 */
@Mapper
public interface SysVersionLogMapper extends BaseMapper<SysVersionLog> {

}
```

---

## 3. Service层（标准实现）

### 3.1 Service接口

**文件**: `flexboot4-admin/src/main/java/com/yunlbd/flexboot4/service/ops/SysVersionLogService.java`

```java
/**
 * 服务层
 *
 * @author Wangts
 * @since 1.0.0
 */
public interface SysVersionLogService extends IExtendedService<SysVersionLog> {

}
```

### 3.2 Service实现

**文件**: `flexboot4-admin/src/main/java/com/yunlbd/flexboot4/service/ops/impl/SysVersionLogServiceImpl.java`

```java
/**
 * 服务层实现
 *
 * @author Wangts
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sysVersionLog")
public class SysVersionLogServiceImpl extends BaseServiceImpl<SysVersionLogMapper, SysVersionLog> implements SysVersionLogService {

}
```

---

## 4. Controller层（标准实现）

**文件**: `flexboot4-admin/src/main/java/com/yunlbd/flexboot4/controller/ops/SysVersionLogController.java`

```java
/**
 * 版本日志控制层
 *
 * @author Wangts
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/version-log")
@RequiredArgsConstructor
@Tag(name = "版本日志", description = "SysVersionLog - 系统版本更新日志")
public class SysVersionLogController extends BaseController<SysVersionLogService, SysVersionLog, String> {

    @Override
    protected Class<SysVersionLog> getEntityClass() {
        return SysVersionLog.class;
    }
}
```

**Controller 继承 BaseController 后自动拥有的能力**：
- `GET /{id}` - 根据ID查询
- `POST /page` - 分页查询
- `POST` - 创建
- `PUT` - 修改
- `DELETE /{id}` - 删除
- `PUT /batch` - 批量修改

**如需扩展自定义接口**，在Controller中添加即可。

---

## 5. 字典配置（可选）

在字典管理中添加 `version_log_type` 字典：

| 字典码 | 字典项 |
|--------|--------|
| version_log_type | feature:新功能, bug_fix:缺陷修复, optimization:性能优化, document:文档更新 |

---

## 6. 菜单配置 (SQL)

```sql
-- 版本日志菜单
INSERT INTO sys_menu (id, parent_id, name, perms, type, path, icon, order_num, create_time, update_time)
VALUES ('version_log', 'sys', '版本日志', 'versionLog:list', 'C', 'version-log', 'version', 10, NOW(), NOW());

-- 按钮权限
INSERT INTO sys_menu (id, parent_id, name, perms, type, create_time)
VALUES ('version_log:query', 'version_log', '查询', 'versionLog:query', 'F', NOW());
INSERT INTO sys_menu (id, parent_id, name, perms, type, create_time)
VALUES ('version_log:add', 'version_log', '新增', 'versionLog:add', 'F', NOW());
INSERT INTO sys_menu (id, parent_id, name, perms, type, create_time)
VALUES ('version_log:edit', 'version_log', '修改', 'versionLog:edit', 'F', NOW());
INSERT INTO sys_menu (id, parent_id, name, perms, type, create_time)
VALUES ('version_log:delete', 'version_log', '删除', 'versionLog:delete', 'F', NOW());
```

---

## 7. 测试验证

### 7.1 标准CRUD接口（BaseController自带）

```bash
# 创建版本日志
POST /api/admin/version-log
Content-Type: application/json
{
  "versionNo": "v1.0.0",
  "releaseDate": "2026-01-01T00:00:00",
  "type": "FEATURE",
  "title": "首个版本发布",
  "content": "## 新增功能\n- 用户登录\n- 权限管理",
  "status": 0
}

# 分页查询（自动按createTime倒序，如需按releaseDate倒序可在SearchDto中指定）
POST /api/admin/version-log/page
{
  "pageNumber": 1,
  "pageSize": 10,
  "orders": [{"column": "releaseDate", "asc": false}]
}

# 根据ID查询
GET /api/admin/version-log/{id}

# 修改
PUT /api/admin/version-log
{
  "id": "xxx",
  "title": "更新的标题"
}

# 删除
DELETE /api/admin/version-log/{id}
```

---

## 📦 文件清单

| 层级 | 文件路径 | 操作 |
|------|----------|------|
| 枚举 | `common/enums/VersionLogType.java` | 新增 |
| 实体 | `entity/ops/SysVersionLog.java` | 新增 |
| Mapper | `mapper/SysVersionLogMapper.java` | 新增 |
| Service | `service/ops/SysVersionLogService.java` | 新增 |
| ServiceImpl | `service/ops/impl/SysVersionLogServiceImpl.java` | 新增 |
| Controller | `controller/ops/SysVersionLogController.java` | 新增 |
| DDL | `resources/db/sys_version_log.sql` | 新增 |

---

## 🚀 执行步骤

1. **执行DDL** - 创建数据库表和索引
2. **创建枚举类** - VersionLogType
3. **创建实体类** - SysVersionLog（继承BaseEntity）
4. **创建Mapper** - SysVersionLogMapper（继承BaseMapper）
5. **创建Service** - SysVersionLogService（继承IExtendedService）
6. **创建ServiceImpl** - SysVersionLogServiceImpl（继承BaseServiceImpl）
7. **创建Controller** - SysVersionLogController（继承BaseController）
8. **执行菜单SQL** - 添加菜单和权限
9. **测试验证** - 验证标准CRUD接口

---

## ✅ 验收标准

- [ ] 遵循 FlexBoot4 标准三层架构（参照 SysDictTypeController）
- [ ] 版本日志支持完整的CRUD操作
- [ ] 分页接口按发布时间倒序返回
- [ ] 支持按状态筛选（草稿/已发布）
- [ ] Swagger文档完整
- [ ] 菜单和权限配置正确
