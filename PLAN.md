# FlexBoot4 Admin 底座架构优化（分阶段路线）

## Summary
- 当前 `admin-starter` 同时承载“底座能力 + 业务能力 + 基础设施集成”，模块职责过重，导致下游 starter（kb/media/cms/sms）对其高耦合，扩展成本高。
- 安全边界存在明显风险：白名单放行了整个 `/api/admin/auth/**`，而 `admin/reset-password` 在该路径下且无权限注解，属于匿名可达高危接口。证据见 [flexboot4-admin-defaults.yml](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/resources/flexboot4-admin-defaults.yml:18) 与 [AuthController.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/controller/sys/AuthController.java:112)。
- 权限拦截器默认“放行非 BaseController 且无注解方法”，导致运维类接口易漏控。证据见 [PermissionCheckInterceptor.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/security/PermissionCheckInterceptor.java:80)、[SysOperLogController.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/controller/ops/SysOperLogController.java:68)、[SysMonitorController.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/controller/ops/SysMonitorController.java:43)。
- 可用性链路有丢数风险：操作日志 Stream 消费先写去重键再落库，落库失败时消息重试会被去重吞掉。证据见 [OperLogStreamListener.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/listener/OperLogStreamListener.java:111)。
- 可维护性问题集中在“实现耦合 + 注入规范 + 类型一致性”：`@Autowired` 字段注入、依赖 `*Impl`、`SysUserRole` 与服务层 ID 类型不一致。证据见 [BaseController.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/controller/sys/BaseController.java:55)、[AiApiKeySnapshotTask.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/task/AiApiKeySnapshotTask.java:12)、[SysUserRole.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/entity/sys/SysUserRole.java:22) 与 [SysUserRoleServiceImpl.java](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/java/com/yunlbd/flexboot4/service/sys/impl/SysUserRoleServiceImpl.java:37)。
- 测试基线不稳：`admin-starter:test` 当前因缺少 JUnit Platform launcher 无法启动测试进程；构建文件也未声明 `testRuntimeOnly`。证据见 [build.gradle.kts](E:/flexboot4/flexboot4/flexboot4-admin-starter/build.gradle.kts:60)。

## Implementation Changes

### 阶段一（P0，1-2 个迭代）：先做安全与正确性兜底
- 收紧白名单策略：将 `/api/admin/auth/**` 改为最小放行集合，仅保留 `login`、`forget-password`、`reset-password`，其余认证接口走鉴权链。
- 给 `admin/reset-password`、`/api/admin/oper-log/page`、`/api/admin/monitor/stats` 增加显式权限注解，并在菜单权限中补齐对应权限码。
- 将权限默认策略改为“`/api/admin/**` 默认拒绝”，仅 `@RequirePermission` 或显式 `skip=true` 放行；避免无注解漏控。
- 移除“明文新密码邮件通知”流程，管理员重置改为“发一次性重置链接”。
- 修复 `SysUserRole` ID 类型一致性（统一为 `String` 并清理 `Long.valueOf` 转换）。
- 去掉监控接口里的 `Thread.sleep(500)`，改为缓存短周期指标或非阻塞采样；查询构建器 `info` 级日志下调为 `debug`。
- 补齐测试运行依赖并恢复 `:flexboot4-admin-starter:test` 通过。

### 阶段二（P1，2-4 个迭代）：底座解耦与 Starter 化
- 引入 `admin-kernel` 子模块（或等价模块）承载“下游公共底座”：`BaseEntity`、`IExtendedService`、`BaseServiceImpl`、`BaseController`、查询构建器；`admin-starter` 聚焦 RBAC/运维能力。
- kb/media/cms/sms 从“依赖整包 admin-starter”切换为“依赖 admin-kernel + 自身能力模块”，降低非必要传递依赖。
- 完成 Boot 标准自动装配：新增 `@AutoConfiguration` + `AutoConfiguration.imports`，移除对 `scanBasePackages("com.yunlbd.flexboot4")` 的强依赖；保留兼容层一个版本周期。
- 基础设施能力按需装配：MinIO、Mail、OperLog Stream、定时任务全部加 `@ConditionalOnProperty`/`@ConditionalOnClass`，默认关闭高外部依赖功能。
- 清理实现耦合：任务层与服务层统一依赖接口，不再注入 `*Impl`；`BaseController` 改为构造器注入。

### 阶段三（P2，持续治理）：高可用与架构约束固化
- 重做 OperLog 消费可靠性：引入 `event_id` 幂等字段（唯一索引），落库成功后再 ack；实现 pending reclaim（`XAUTOCLAIM`）处理崩溃消费者遗留消息。
- 为 `@Scheduled` 任务加分布式锁（ShedLock/Redis Lock），避免多实例重复执行。
- 引入 Flyway 统一管理 starter SQL 演进，消除“启动后才暴露缺表”的部署风险。
- 加入 ArchUnit 规则：禁止字段注入、禁止跨层依赖实现类、限制模块反向依赖。
- 标准化可观测性：关键链路增加指标（认证失败率、权限拒绝率、日志消费延迟、重试次数）与告警阈值。

## Public APIs / Interfaces / Types 变更点
- 新增模块边界：`admin-kernel`（公共底座）与 `admin-starter`（业务能力）分离，`admin-starter` 保留兼容聚合入口。
- 安全配置从“通配白名单”升级为“显式端点白名单 + 默认拒绝策略”。
- 认证重置接口行为调整：不再发送明文密码，统一 token-link 流程。
- 运维接口权限模型显式化：`monitor` 与 `oper-log` 接口要求独立权限码。
- 自动装配入口标准化：从 `spring.factories` 监听器补充为 Boot AutoConfiguration 机制。证据见 [spring.factories](E:/flexboot4/flexboot4/flexboot4-admin-starter/src/main/resources/META-INF/spring.factories:1)。

## Test Plan
- 安全回归：匿名访问 `admin/reset-password`、`monitor/stats`、`oper-log/page` 必须拒绝；具备权限用户必须通过。
- 权限回归：非 BaseController 接口在无注解时默认拒绝；`skip=true` 接口可控放行。
- 可靠性回归：模拟 OperLog 落库失败与消费者重启，验证“不丢消息、可重试、最终一致”。
- 模块回归：仅引入 `admin-kernel` 的下游模块可独立编译运行；引入 `admin-starter` 保持旧行为。
- 测试基线：`./gradlew :flexboot4-admin-starter:test` 稳定通过，并纳入 CI 必过门禁。

## Assumptions
- 保持 Java 25、Spring Boot 4、MyBatis-Flex 技术栈不变。
- 对外 HTTP 接口路径尽量保持兼容，优先做安全加固与权限收敛。
- `admin-starter` 兼容入口至少保留一个发布周期，逐步引导下游迁移到新边界。
- `flexboot4-core` 继续保持纯 Java，不承载 Spring/ORM 依赖。
