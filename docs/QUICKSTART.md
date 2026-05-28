# 快速开始

本文档保留为轻量入口。完整开发规范请阅读 [开发者指南](guide.md)。

## 1. 启动后端聚合应用

```powershell
.\gradlew.bat :flexboot4-bootstrap:bootRun
```

API 文档：

```text
http://localhost:8080/scalar
```

## 2. 后端验证

```powershell
.\gradlew.bat compileJava test
```

本地后端启动后，可以执行 Admin 冒烟脚本验证登录、权限码、vben 动态路由和核心列表接口：

```powershell
.\scripts\admin-smoke.ps1 -BaseUrl "http://localhost:8080" -Username admin -Password admin123
```

如需验证通用 CRUD 写入链路，可追加 `-Crud`。该模式会创建并清理临时字典类型和字典项：

```powershell
.\scripts\admin-smoke.ps1 -BaseUrl "http://localhost:8080" -Username admin -Password admin123 -Crud
```

默认文件存储为本地磁盘，路径为 `${user.home}/flexboot4-files`。如需切换到 MinIO：

```powershell
$env:FLEXBOOT4_FILE_STORAGE_TYPE="minio"
```

MinIO 连接参数继续使用 `minio.endpoint`、`minio.access-key`、`minio.secret-key`、`minio.bucket` 等配置。

本地存储访问走短期签名 URL，不暴露静态目录。文件管理页通过 `/api/admin/file/{id}/access-url` 获取访问地址；头像和 CMS 文件应使用各自业务接口，避免依赖系统文件管理权限。

如果启用或测试个人中心 MFA 设备，生产和多人联调环境应显式注入稳定的 MFA 加密密钥：

```powershell
$env:FLEXBOOT4_SECURITY_MFA_SECRET_KEY="replace-with-a-random-32-byte-or-longer-secret"
```

该密钥用于加密 `sys_user_mfa` 中的 TOTP secret。不要提交到仓库，同一环境的所有后端实例必须保持一致；修改后已绑定 MFA 的用户需要重新绑定或执行专门迁移。更多说明见 [Admin 认证与账号安全](admin-auth-security.md)。

## 3. 前端验证

前端 vben 工程位于同级目录 `flexboot-web/vue-vben-admin`。

```powershell
pnpm -F @vben/web-antd typecheck
```

## 4. 外部项目引入

```kotlin
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))

    implementation("com.yunlbd:flexboot4-admin-starter")
    // implementation("com.yunlbd:flexboot4-cms-starter")
    // implementation("com.yunlbd:flexboot4-media-starter")
    // implementation("com.yunlbd:flexboot4-sms4j-starter")
    // implementation("com.yunlbd:flexboot4-kb-starter")
}
```

业务应用不需要配置 `scanBasePackages("com.yunlbd.flexboot4")`。如果业务应用有自己的 Mapper，只扫描业务包即可。

## 5. 继续阅读

- [开发者指南](guide.md)
- [Admin 认证与账号安全](admin-auth-security.md)
- [模块文档索引](modules.md)
- [Starter 架构](STARTER_ARCHITECTURE.md)
- [权限设计](backend_permission_control_design.md)
- [通用查询](Mf基础功能.md)
