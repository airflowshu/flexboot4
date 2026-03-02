# 🚀 快速 Git 设置指南

## 已完成 ✅
- ✅ Git 仓库已重新初始化 (`git init`)
- ✅ 所有文件已添加到暂存区 (`git add .`)
- ✅ GitHub Actions 工作流已创建

## 3 步快速完成设置

### 步骤 1：配置 Git 用户信息

在 PowerShell 中运行：

```powershell
git config user.name "你的GitHub用户名"
git config user.email "你的邮箱@example.com"
```

### 步骤 2：创建初始提交

```powershell
git commit -m "Initial commit"
git branch -M main
```

### 步骤 3：关联到 GitHub 并推送

将 `YOUR_USERNAME` 替换为你的 GitHub 用户名：

**方式 A：HTTPS（推荐新手）**
```powershell
git remote add origin https://github.com/YOUR_USERNAME/flexboot4.git
git push -u origin main
```

**方式 B：SSH（更安全）**
```powershell
git remote add origin git@github.com:YOUR_USERNAME/flexboot4.git
git push -u origin main
```

---

## 自动化脚本方式 (推荐)

如果你觉得手工步骤太复杂，可以使用提供的 PowerShell 脚本：

### HTTPS 方式：
```powershell
.\setup-github.ps1 -GitHubUsername "YOUR_USERNAME" -GitHubEmail "your@email.com"
```

### SSH 方式：
```powershell
.\setup-github.ps1 -GitHubUsername "YOUR_USERNAME" -GitHubEmail "your@email.com" -UseSSH
```

---

## 📦 GitHub Pages 文档自动部署

已为你创建了 `.github/workflows/deploy-docs.yml` 文件

**自动构建和部署流程：**
1. 当你推送代码到 `main` 分支
2. GitHub Actions 自动触发
3. 安装依赖 (`pnpm install`)
4. 构建文档 (`pnpm docs:build`)
5. 部署到 GitHub Pages 的 `/flexboot4` 路径

**配置 GitHub Pages：**
1. 进入 GitHub 仓库 → Settings
2. 找到 Pages 选项
3. Source 选择：`Deploy from a branch`
4. Branch 选择：`gh-pages` 和 `/ (root)`
5. 保存

---

## ✨ 验证步骤

完成后，验证一切是否正确：

```powershell
# 查看 Git 配置
git config user.name
git config user.email

# 查看远程仓库
git remote -v

# 查看分支
git branch -a

# 查看日志
git log --oneline -n 5
```

---

## 🆘 常见问题

### Q: "fatal: not a git repository"
**A:** 运行 `git init` 重新初始化仓库

### Q: "remote origin already exists"
**A:** 运行 `git remote remove origin` 然后重新添加

### Q: 推送时要求认证
**A:** 
- **HTTPS**：输入 GitHub 用户名和 Personal Access Token
  - 生成 Token: GitHub → Settings → Developer settings → Personal access tokens
- **SSH**：确保已配置 SSH 密钥

### Q: "Please tell me who you are"
**A:** 运行 `git config user.name` 和 `git config user.email`

### Q: 需要覆盖远程历史
**A:** 运行 `git push -f origin main` （谨慎使用！）

---

## 📚 相关文件

- `GIT_SETUP_INSTRUCTIONS.md` - 详细的设置指南
- `.github/workflows/deploy-docs.yml` - GitHub Actions 工作流配置
- `setup-github.ps1` - 自动化设置脚本

---

## 📝 后续 Git 常用命令

```powershell
# 查看状态
git status

# 添加更改
git add .

# 提交更改
git commit -m "你的提交信息"

# 推送到 GitHub
git push

# 拉取最新代码
git pull

# 创建新分支
git checkout -b feature/新功能名称

# 切换分支
git checkout 分支名称

# 查看分支列表
git branch -a
```

---

祝你使用愉快！ 🎉

