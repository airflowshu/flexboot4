# Flexboot4 项目文档

欢迎来到 Flexboot4 项目！

## 📚 文档

- **[完整文档](./flexboot4/index.html)** - 访问完整的项目文档

## 🚀 快速开始

本项目使用 VitePress 构建的现代化文档站点。

### 本地开发

1. **进入 doc 目录**
   ```bash
   cd doc
   ```

2. **安装依赖**
   ```bash
   pnpm install
   ```

3. **启动开发服务器**
   ```bash
   pnpm docs:dev
   ```

   访问 `http://localhost:5173` 查看文档

4. **构建生产版本**
   ```bash
   pnpm docs:build
   ```

## 📖 项目结构

```
flexboot4/
├── doc/                    # 文档源文件
│   ├── src/
│   │   ├── index.md       # 首页
│   │   ├── guide.md       # 使用指南
│   │   ├── API_TAG_GROUP_GUIDE.md
│   │   └── ...
│   ├── .vitepress/        # VitePress 配置
│   │   ├── config.ts
│   │   ├── dist/          # 构建输出（GitHub Pages 部署源）
│   │   └── ...
│   └── package.json
├── flexboot4-core/        # 核心模块
├── flexboot4-admin-starter/
├── flexboot4-ai/
├── flexboot4-bootstrap/
├── flexboot4-kb-starter/
├── flexboot4-media-starter/
└── ...
```

## 🔄 自动部署流程

本项目配置了 GitHub Actions 工作流，在以下情况下自动构建和部署文档：

1. **触发条件**：向 `main` 分支推送代码
2. **构建步骤**：
   - 安装依赖（使用 pnpm）
   - 构建文档（运行 `pnpm docs:build`）
3. **部署**：自动部署到 GitHub Pages 的 `/flexboot4` 路径

## 🛠️ 技术栈

- **VitePress** - 静态网站生成器
- **Markdown** - 文档编写
- **Node.js** - 运行环境
- **pnpm** - 包管理器
- **GitHub Actions** - CI/CD 自动化
- **GitHub Pages** - 文档托管

## 📝 编写文档

所有文档文件都是 Markdown 格式（`.md`）。

### 添加新文档

1. 在 `doc/src/` 目录下创建 `.md` 文件
2. 在 `.vitepress/config.ts` 中的导航菜单配置中添加链接
3. 推送代码，GitHub Actions 会自动部署

### Markdown 语法

- [Markdown 基础语法](https://guides.github.com/features/mastering-markdown/)
- [VitePress Markdown 扩展](https://vitepress.dev/guide/markdown)

## 🐛 常见问题

### 文档构建失败

**错误**: `Element is missing end tag`

**原因**: 某些 Markdown 文件中的 Vue 组件或 HTML 标签没有正确闭合

**解决方案**:
- 检查 `doc/` 目录下的所有 `.md` 文件
- 确保所有 HTML 标签都正确闭合
- 检查特殊字符是否需要转义

### 本地开发和生产不一致

运行 `pnpm docs:build` 并检查输出，这与 GitHub Pages 上的显示最接近。

## 📦 部署到 GitHub Pages 的其他分支

默认配置将文档部署到 `/flexboot4` 子目录。如果你需要部署到其他路径，编辑：

```bash
.github/workflows/deploy-docs.yml
```

修改 `destination_dir` 参数。

## 🔗 相关链接

- [项目 GitHub 仓库](https://github.com/YOUR_USERNAME/flexboot4)
- [VitePress 官方文档](https://vitepress.dev)
- [GitHub Pages 帮助](https://docs.github.com/pages)

## 📄 许可证

项目许可证信息，详见 LICENSE 文件

---

**最后更新**: 2026-03-02

需要帮助？请创建一个 Issue 或联系项目维护者。

