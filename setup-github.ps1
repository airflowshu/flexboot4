#!/usr/bin/env pwsh

# Git 仓库初始化和 GitHub 关联脚本
# 使用方式: ./setup-github.ps1

param(
    [string]$GitHubUsername,
    [string]$GitHubEmail,
    [switch]$UseSSH = $false
)

# 颜色输出函数
function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor Yellow
}

# 检查必要的参数
if (-not $GitHubUsername) {
    Write-Info "请提供 GitHub 用户名"
    $GitHubUsername = Read-Host "GitHub 用户名"
}

if (-not $GitHubEmail) {
    Write-Info "请提供 GitHub 邮箱"
    $GitHubEmail = Read-Host "GitHub 邮箱"
}

# 获取当前目录
$CurrentDir = Get-Location
Write-Info "当前目录: $CurrentDir"

# 配置 Git
Write-Info "配置 Git 用户信息..."
git config user.name $GitHubUsername
git config user.email $GitHubEmail
Write-Success "Git 用户配置完成"

# 添加所有文件
Write-Info "添加所有文件到暂存区..."
git add .
Write-Success "文件已添加"

# 创建初始提交
Write-Info "创建初始提交..."
git commit -m "Initial commit"
Write-Success "初始提交已创建"

# 创建/切换到 main 分支
Write-Info "创建/切换到 main 分支..."
git branch -M main
Write-Success "已切换到 main 分支"

# 添加远程仓库
Write-Info "添加远程仓库..."
$RepoUrl = if ($UseSSH) {
    "git@github.com:$GitHubUsername/flexboot4.git"
} else {
    "https://github.com/$GitHubUsername/flexboot4.git"
}

git remote add origin $RepoUrl
Write-Success "远程仓库已添加: $RepoUrl"

# 验证远程仓库
Write-Info "验证远程仓库配置..."
git remote -v
Write-Success "远程仓库验证完成"

# 推送到 GitHub
Write-Warning-Custom "准备推送到 GitHub..."
Write-Info "执行: git push -u origin main"

Write-Info "第一次推送需要进行身份验证。如果使用 HTTPS，请输入 GitHub 用户名和 Personal Access Token。"
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Success "推送成功!"
    Write-Success "✓ Git 仓库已成功初始化并关联到 GitHub"
    Write-Info "仓库地址: $RepoUrl"
    Write-Info ""
    Write-Info "接下来你可以:"
    Write-Info "1. 在 GitHub 仓库设置中启用 GitHub Pages"
    Write-Info "2. 选择 gh-pages 分支作为发布源"
    Write-Info "3. 设置自定义域名（如果需要）"
} else {
    Write-Error-Custom "推送失败!"
    Write-Info "可能的原因:"
    Write-Info "- 身份验证失败（检查用户名和 token）"
    Write-Info "- 仓库已在 GitHub 上存在且有不同的历史"
    Write-Info ""
    Write-Info "解决方案:"
    Write-Info "1. 检查 GitHub 连接: git remote -v"
    Write-Info "2. 如果需要覆盖远程历史: git push -f origin main"
}

