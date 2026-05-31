$ErrorActionPreference = 'Stop'

$repoRoot = (git -C $PSScriptRoot rev-parse --show-toplevel).Trim()
$hooksPath = '.githooks'
$prePushPath = Join-Path $repoRoot '.githooks\pre-push'

if (-not (Test-Path -LiteralPath $prePushPath)) {
    throw "pre-push hook not found: $prePushPath"
}

git -C $repoRoot config core.hooksPath $hooksPath

$configuredPath = (git -C $repoRoot config --get core.hooksPath).Trim()

Write-Host "Git hooks installed for repository: $repoRoot"
Write-Host "core.hooksPath = $configuredPath"
Write-Host "IDEA and command-line git push will now run .githooks/pre-push."
Write-Host "Temporary skip: `$env:SKIP_DOCS_BUILD='1'; git push"
