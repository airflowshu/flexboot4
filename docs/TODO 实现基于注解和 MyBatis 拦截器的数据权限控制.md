# 数据权限控制方案（Java 25 ScopedValue + 自定义部门支持）

基于您的要求，本方案将充分利用 Java 25 的标准特性 `ScopedValue` 来替代传统的 `ThreadLocal`，实现更安全、高效的上下文传递，并完整支持包括“自定义部门”在内的 5 种数据权限策略。

## 1. 环境与模型调整

### 1.1 构建配置更新

* 修改 `build.gradle.kts`，将 `languageVersion` 升级为 `Java 25`，确保能直接使用 `ScopedValue` 标准特性。

### 1.2 数据库与实体变更

1. **SysRole 表**：增加 `data_scope` 字段（CHAR/VARCHAR）。

   * 字典：`1`=全部, `2`=本部门及以下, `3`=本部门, `4`=仅本人, `5`=自定义
2. **SysRoleDept 表**（新增）：用于存储角色与部门的关联（当 data\_scope=5 时）。

   * 字段：`role_id`, `dept_id`
3. **SysRole 实体**：

   * 增加 `String dataScope` 字段。

   * 增加 `List<SysDept> customDepts` 字段，并配置 `@RelationManyToMany` 映射到 `sys_role_dept` 表。

## 2. 核心组件设计

### 2.1 上下文管理 (`DataScopeContext`)

* 定义全局常量：`public static final ScopedValue<DataScopeContext> SCOPED_CONTEXT = ScopedValue.newInstance();`

* 利用 `ScopedValue` 的**不可变性**和**结构化并发**特性，彻底杜绝上下文未清理导致的内存泄漏问题。

### 2.2 注解定义 (`@DataScope`)

* 保持原有设计：`deptAlias`, `userAlias`。

### 2.3 切面处理器 (`DataScopeAspect`)

* 采用 `@Around` 环绕通知：

  1. 解析注解，结合当前用户角色计算 SQL 条件。
  2. **自定义逻辑**：若权限为“自定义”，通过 `sys_role_dept` 关联表查询该角色拥有的部门 ID 集合，生成 `dept_id IN (...)`。
  3. **混合注入**：

     * 若有 `QueryWrapper`，直接注入。

     * 否则，准备 SQL 片段。
  4. **作用域绑定**：
     使用 `ScopedValue.callWhere(SCOPED_CONTEXT, context, () -> point.proceed())` 执行目标方法。
     这确保了权限上下文仅在当前方法调用链中有效，天然线程安全。

### 2.4 MyBatis 拦截器 (`DataScopeInterceptor`)

* 拦截 `Executor.query`。

* 通过 `SCOPED_CONTEXT.orElse(null)` 获取当前上下文。

* 若存在上下文，修改 `BoundSql` 拼接 SQL 条件。

## 3. 实施步骤

1. **环境升级**：更新 `build.gradle.kts` 至 Java 25。
2. **模型扩展**：

   * 更新 `SysRole.java`。

   * 创建 `SysRoleDept.java` (可选，或仅用中间表配置)。

   * 提供 SQL 脚本（需要）。
3. **核心代码**：

   * 实现 `DataScopeContext` (基于 `ScopedValue`)。

   * 实现 `DataScopeAspect` (处理自定义部门逻辑)。

   * 实现 `DataScopeInterceptor`。
4. **验证**：在 `SysUserServiceImpl` 中演示效果。

此方案完美结合了业务需求（自定义部门）与前沿技术（Java 25 ScopedValue），实现了高性能、高安全性的权限控制。
