@echo off
REM Git 仓库初始化和 GitHub 关联 - 批处理脚本
REM 使用方式: setup-github.bat

setlocal enabledelayedexpansion

echo ================================================
echo   Flexboot4 Git 仓库初始化脚本
echo ================================================
echo.

REM 检查 Git 是否已安装
git --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Git 未安装或未在 PATH 中
    echo 请访问 https://git-scm.com/ 下载安装 Git
    exit /b 1
)

echo [成功] Git 已安装
echo.

REM 获取 GitHub 用户名
set /p GITHUB_USERNAME="请输入你的 GitHub 用户名: "
if "!GITHUB_USERNAME!"=="" (
    echo [错误] 用户名不能为空
    exit /b 1
)

REM 获取 GitHub 邮箱
set /p GITHUB_EMAIL="请输入你的 GitHub 邮箱: "
if "!GITHUB_EMAIL!"=="" (
    echo [错误] 邮箱不能为空
    exit /b 1
)

REM 选择连接方式
echo.
echo 请选择 Git 连接方式:
echo   1) HTTPS (推荐，使用用户名和 Token)
echo   2) SSH (需要提前配置 SSH 密钥)
set /p CONNECTION_TYPE="请选择 (1或2): "

echo.
echo 开始配置...
echo.

REM 配置 Git 用户信息
echo [步骤 1] 配置 Git 用户信息...
git config user.name "!GITHUB_USERNAME!"
git config user.email "!GITHUB_EMAIL!"
if !errorlevel! equ 0 (
    echo [成功] Git 用户配置完成
) else (
    echo [错误] Git 用户配置失败
    exit /b 1
)

echo.

REM 添加所有文件
echo [步骤 2] 添加所有文件到暂存区...
git add .
if !errorlevel! equ 0 (
    echo [成功] 文件已添加
) else (
    echo [错误] 添加文件失败
    exit /b 1
)

echo.

REM 创建初始提交
echo [步骤 3] 创建初始提交...
git commit -m "Initial commit: Initialize project with documentation and GitHub Actions"
if !errorlevel! equ 0 (
    echo [成功] 初始提交已创建
) else (
    echo [警告] 没有新增内容或提交失败
)

echo.

REM 创建/切换到 main 分支
echo [步骤 4] 创建/切换到 main 分支...
git branch -M main
if !errorlevel! equ 0 (
    echo [成功] 已切换到 main 分支
) else (
    echo [错误] 分支操作失败
    exit /b 1
)

echo.

REM 添加远程仓库
echo [步骤 5] 添加远程仓库...

if "!CONNECTION_TYPE!"=="2" (
    set REPO_URL=git@github.com:!GITHUB_USERNAME!/flexboot4.git
) else (
    set REPO_URL=https://github.com/!GITHUB_USERNAME!/flexboot4.git
)

REM 检查是否已存在 origin
git remote | findstr /c:"origin" >nul 2>&1
if !errorlevel! equ 0 (
    echo [提示] 远程仓库 origin 已存在，尝试更新...
    git remote set-url origin "!REPO_URL!"
) else (
    git remote add origin "!REPO_URL!"
)

if !errorlevel! equ 0 (
    echo [成功] 远程仓库已添加: !REPO_URL!
) else (
    echo [错误] 添加远程仓库失败
    exit /b 1
)

echo.

REM 验证远程仓库
echo [步骤 6] 验证远程仓库配置...
git remote -v
echo.

REM 推送到 GitHub
echo [步骤 7] 准备推送到 GitHub...
echo.
echo 执行命令: git push -u origin main
echo.
if "!CONNECTION_TYPE!"=="1" (
    echo [提示] 使用 HTTPS 需要输入凭证
    echo         用户名: !GITHUB_USERNAME!
    echo         密码: 使用 Personal Access Token (非密码)
    echo         获取 Token: GitHub Settings ^> Developer settings ^> Personal access tokens
) else (
    echo [提示] 使用 SSH 需要已配置的 SSH 密钥
)

echo.
echo 按任意键继续推送或按 Ctrl+C 退出...
pause >nul

git push -u origin main

if !errorlevel! equ 0 (
    echo.
    echo ================================================
    echo   [成功] Git 仓库已成功初始化并关联到 GitHub!
    echo ================================================
    echo.
    echo 仓库地址: !REPO_URL!
    echo.
    echo 接下来你需要:
    echo   1. 在 GitHub 仓库设置中启用 GitHub Pages
    echo   2. 选择 gh-pages 分支作为发布源
    echo   3. 等待 GitHub Pages 部署完成
    echo.
    echo GitHub Pages 访问地址:
    echo   https://!GITHUB_USERNAME!.github.io/flexboot4/
    echo.
    echo 更多信息，请查看:
    echo   - GITHUB_SETUP_COMPLETE.md
    echo   - QUICK_START_GIT.md
    echo.
) else (
    echo.
    echo [错误] 推送失败！
    echo.
    echo 可能的原因:
    echo   - 身份验证失败 (检查用户名和 token/密钥)
    echo   - 仓库已在 GitHub 上存在且有不同的历史
    echo.
    echo 解决方案:
    echo   1. 检查 GitHub 连接: git remote -v
    echo   2. 如果需要覆盖: git push -f origin main
    echo.
    exit /b 1
)

endlocal
pause

