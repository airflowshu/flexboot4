param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [switch]$Crud,
    [switch]$SkipMonitor,
    [switch]$Insecure
)

$ErrorActionPreference = "Stop"

if ($Insecure) {
    [System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
}

$script:Token = $null
$script:CreatedDictTypeId = $null
$script:CreatedDictItemId = $null

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null,
        [int[]]$ExpectedStatus = @(200),
        [switch]$Raw
    )

    $headers = @{}
    if ($script:Token) {
        $headers["Authorization"] = "Bearer $script:Token"
    }

    $uri = "$BaseUrl$Path"
    $json = $null
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 20
    }

    try {
        $response = Invoke-WebRequest -Method $Method -Uri $uri -Headers $headers `
            -Body $json -ContentType "application/json" -UseBasicParsing
    } catch {
        $response = $_.Exception.Response
        if ($null -eq $response) {
            throw
        }
    }

    if ($ExpectedStatus -notcontains [int]$response.StatusCode) {
        throw "[$Method $Path] expected HTTP $($ExpectedStatus -join ',') but got $([int]$response.StatusCode)"
    }

    if ($Raw) {
        return $response
    }

    $content = $response.Content
    if ([string]::IsNullOrWhiteSpace($content)) {
        return $null
    }

    $result = $content | ConvertFrom-Json
    if ($result.PSObject.Properties.Name -contains "code" -and $result.code -ne 0) {
        throw "[$Method $Path] business code=$($result.code), message=$($result.message), error=$($result.error)"
    }
    return $result.data
}

function Step {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )
    Write-Host "[SMOKE] $Name ..." -ForegroundColor Cyan
    & $Action | Out-Null
    Write-Host "[SMOKE] $Name OK" -ForegroundColor Green
}

try {
    Step "login" {
        $data = Invoke-Api -Method "POST" -Path "/api/admin/auth/login" -Body @{
            username = $Username
            password = $Password
        }
        if (-not $data.accessToken) {
            throw "login response missing accessToken"
        }
        $script:Token = $data.accessToken
    }

    Step "permission codes" {
        $codes = Invoke-Api -Method "GET" -Path "/api/admin/auth/codes"
        if ($null -eq $codes) {
            throw "permission codes response is null"
        }
    }

    Step "vben dynamic routes" {
        $routes = Invoke-Api -Method "GET" -Path "/api/admin/menu/all"
        if ($null -eq $routes) {
            throw "route response is null"
        }
    }

    Step "user page with root alias order" {
        Invoke-Api -Method "POST" -Path "/api/admin/user/page" -Body @{
            pageNumber = 1
            pageSize = 5
            items = @()
            orders = @(@{ column = "sysUser.createTime"; asc = $false })
        }
    }

    Step "role list" {
        Invoke-Api -Method "POST" -Path "/api/admin/role/list" -Body @{
            pageNumber = 1
            pageSize = 20
            items = @()
            orders = @(@{ column = "createTime"; asc = $false })
        }
    }

    Step "menu list" {
        Invoke-Api -Method "POST" -Path "/api/admin/menu/list" -Body @{
            pageNumber = 1
            pageSize = 200
            items = @()
            orders = @(@{ column = "orderNo"; asc = $true })
        }
    }

    Step "dict type page" {
        Invoke-Api -Method "POST" -Path "/api/admin/dict-type/page" -Body @{
            pageNumber = 1
            pageSize = 10
            items = @()
            orders = @(@{ column = "createTime"; asc = $false })
        }
    }

    Step "config page" {
        Invoke-Api -Method "POST" -Path "/api/admin/config/page" -Body @{
            pageNumber = 1
            pageSize = 10
            items = @()
            orders = @(@{ column = "createTime"; asc = $false })
        }
    }

    Step "api key page" {
        Invoke-Api -Method "POST" -Path "/api/admin/api-key/page" -Body @{
            pageNumber = 1
            pageSize = 10
            items = @()
            orders = @(@{ column = "createTime"; asc = $false })
        }
    }

    if (-not $SkipMonitor) {
        Step "monitor stats" {
            Invoke-Api -Method "GET" -Path "/api/admin/monitor/stats"
        }
    }

    if ($Crud) {
        $suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        Step "dict type create" {
            $ok = Invoke-Api -Method "POST" -Path "/api/admin/dict-type" -Body @{
                code = "smoke_$suffix"
                name = "Smoke $suffix"
                status = 1
                orderNo = 9999
                remark = "created by admin-smoke.ps1"
            }
            if ($ok -ne $true) {
                throw "dict type create returned $ok"
            }
        }

        Step "dict type locate" {
            $page = Invoke-Api -Method "POST" -Path "/api/admin/dict-type/page" -Body @{
                pageNumber = 1
                pageSize = 1
                items = @(@{ field = "sysDictType.code"; op = "eq"; val = "smoke_$suffix" })
                orders = @()
            }
            if (-not $page.records -or -not $page.records[0].id) {
                throw "created dict type not found"
            }
            $script:CreatedDictTypeId = $page.records[0].id
        }

        Step "dict type update" {
            $ok = Invoke-Api -Method "PUT" -Path "/api/admin/dict-type/$script:CreatedDictTypeId" -Body @{
                name = "Smoke Updated $suffix"
                status = 1
                orderNo = 9998
                remark = "updated by admin-smoke.ps1"
            }
            if ($ok -ne $true) {
                throw "dict type update returned $ok"
            }
        }

        Step "dict item create" {
            $ok = Invoke-Api -Method "POST" -Path "/api/admin/dict-item" -Body @{
                typeId = $script:CreatedDictTypeId
                itemCode = "item_$suffix"
                itemText = "Smoke Item"
                itemValue = "1"
                status = 1
                orderNo = 1
                remark = "created by admin-smoke.ps1"
            }
            if ($ok -ne $true) {
                throw "dict item create returned $ok"
            }
        }

        Step "dict item locate" {
            $items = Invoke-Api -Method "POST" -Path "/api/admin/dict-item/list" -Body @{
                pageNumber = 1
                pageSize = 10
                items = @(@{ field = "typeId"; op = "eq"; val = $script:CreatedDictTypeId })
                orders = @()
            }
            if (-not $items -or -not $items[0].id) {
                throw "created dict item not found"
            }
            $script:CreatedDictItemId = $items[0].id
        }

        Step "dict item delete" {
            Invoke-Api -Method "DELETE" -Path "/api/admin/dict-item/$script:CreatedDictItemId"
            $script:CreatedDictItemId = $null
        }

        Step "dict type delete" {
            Invoke-Api -Method "DELETE" -Path "/api/admin/dict-type/$script:CreatedDictTypeId"
            $script:CreatedDictTypeId = $null
        }
    }

    Write-Host "[SMOKE] all checks passed" -ForegroundColor Green
} finally {
    if ($script:Token) {
        try {
            if ($script:CreatedDictItemId) {
                Invoke-Api -Method "DELETE" -Path "/api/admin/dict-item/$script:CreatedDictItemId" | Out-Null
            }
            if ($script:CreatedDictTypeId) {
                Invoke-Api -Method "DELETE" -Path "/api/admin/dict-type/$script:CreatedDictTypeId" | Out-Null
            }
        } catch {
            Write-Warning "cleanup failed: $($_.Exception.Message)"
        }
    }
}
