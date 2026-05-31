# 数据库迁移与 Flyway

FlexBoot4 默认项目形态包含 `flexboot4-admin-starter`，因此 Flyway 集成在 Admin Starter 中，而不是下沉到 `flexboot4-admin-kernel`。Kernel 只承载公共代码能力，不应该产生数据库初始化副作用；Admin 负责 RBAC、菜单、权限、系统配置，也最适合作为后台项目的数据库治理入口。

## 默认行为

引入 `flexboot4-admin-starter` 后，项目会自动获得 Flyway runtime 和 PostgreSQL 支持：

```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter")
    implementation("com.yunlbd:flexboot4-sms4j-starter")
}
```

Admin 会在启动早期扫描 classpath 上所有模块的 `META-INF/flexboot4/flyway-module.properties`，把已引入模块的迁移目录合并到 `spring.flyway.locations`。业务项目自己的迁移脚本仍放在 Spring Boot/Flyway 默认目录：

```text
classpath:db/migration
```

FlexBoot4 内置模块脚本使用独立目录，避免和业务项目脚本混在一起：

```text
classpath:db/flexboot4-migration/<module>/postgresql
```

## 模块声明

每个带数据库资源的 starter 都提供一个声明文件：

```properties
module=sms4j
database=postgresql
locations=classpath:db/flexboot4-migration/sms4j/postgresql
```

声明文件只表达三件事：模块名、数据库类型、迁移目录。模块在 classpath 上存在时默认参与迁移，不需要在声明文件里写 `enabled=true`。如果需要临时关闭某个模块迁移，用外部配置控制：

```yaml
flexboot4:
  flyway:
    modules:
      sms4j:
        enabled: false
```

这里也不设计 `requires=admin` 和 `order`：

- `requires=admin` 属于构建依赖关系，不属于数据库迁移声明。默认项目形态由业务应用显式引入 `flexboot4-admin-starter`，Admin Starter 负责统一聚合迁移目录。
- `enabled=true` 是默认行为，写进每个模块声明只会增加重复心智负担。是否临时禁用某模块，交给外部配置。
- `order` 不参与 Flyway 的真实执行顺序。Flyway 按版本号排序执行脚本，因此模块之间的先后关系通过全局版本段表达。

## 版本号规则

Flyway 的执行顺序由脚本版本号决定。FlexBoot4 内置模块使用全局版本段，避免不同 starter 都使用 `V1`、`V2` 造成冲突：

```text
V1000__admin_core_schema.sql
V1010__admin_menu_data.sql
V2000__sms4j_schema.sql
V2010__sms4j_menu_data.sql
V3000__cms_schema.sql
V3010__cms_menu_data.sql
V4000__media_schema.sql
V4010__media_menu_data.sql
V5000__kb_schema.sql
V5010__kb_menu_data.sql
```

新增模块时先分配新的版本段；同一模块后续演进只在自己的版本段内递增。

## 配置项

```yaml
flexboot4:
  flyway:
    enabled: true
    database: postgresql
    auto-detect-modules: true
```

- `flexboot4.flyway.enabled`：是否启用 FlexBoot4 模块迁移目录合并；不等同于 `spring.flyway.enabled`。
- `flexboot4.flyway.database`：当前内置迁移数据库类型，默认 `postgresql`。
- `flexboot4.flyway.auto-detect-modules`：是否扫描 classpath 上的模块声明。
- `spring.flyway.enabled`：Spring Boot/Flyway 是否实际执行迁移。

## 新模块接入

1. 在模块资源目录新增迁移脚本：

```text
src/main/resources/db/flexboot4-migration/<module>/postgresql/Vx000__<module>_schema.sql
src/main/resources/db/flexboot4-migration/<module>/postgresql/Vx010__<module>_menu_data.sql
```

2. 新增声明文件：

```text
src/main/resources/META-INF/flexboot4/flyway-module.properties
```

3. 在声明文件中填写：

```properties
module=<module>
database=postgresql
locations=classpath:db/flexboot4-migration/<module>/postgresql
```

4. 增加资源打包测试，确保声明文件和迁移脚本能从 classpath 读取。

## 开发期重置

当前项目仍处开发阶段，不要求兼容旧库历史。若开发库已经手动执行过旧版 `init.sql`、`menu_data.sql` 或旧 Flyway 脚本，推荐重建数据库后让 Flyway 从空库执行：

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

然后重新启动应用，Flyway 会创建 `flyway_schema_history` 并执行已引入模块的迁移脚本。

如果只想清理 Flyway 历史而保留表结构，需要确保现有结构和当前迁移完全一致，否则后续校验或重复执行可能失败。开发阶段优先使用重建库方式。
