# FlexBoot4 Bootstrap

`flexboot4-bootstrap` 是仓库内的集成开发与验证应用，用于聚合 Admin、KB、Media、SMS4J、CMS 等 starter，方便在一个 Spring Boot 进程内完成联调。

## 定位

- 仅用于本仓库内部开发、集成测试与本地演示
- 不作为 Maven artifact 发布
- 不建议外部业务项目直接依赖该模块

## 聚合范围

- `flexboot4-admin-starter`: RBAC、系统管理、操作日志、权限控制
- `flexboot4-kb-starter`: 知识库、文件解析、Embedding 流程
- `flexboot4-media-starter`: 媒体服务、GB28181、ZLM hook、运行时巡检
- `flexboot4-sms4j-starter`: 多通道短信集成
- `flexboot4-cms-starter`: CMS 模板管理与静态发布

## 默认启用项

Bootstrap 会在 `src/main/resources/application.yml` 中显式启用部分外部依赖相关能力，用于完整联调：

```yaml
media:
  enabled: true
  runtime-check-enabled: true

file:
  embedding:
    stream:
      enabled: true
```

外部业务项目直接引入 starter 时，这些运行时任务默认保持关闭，需要按需显式开启。

## 运行

```bash
./gradlew :flexboot4-bootstrap:bootRun
```

## 验证

```bash
./gradlew :flexboot4-bootstrap:compileJava
```
