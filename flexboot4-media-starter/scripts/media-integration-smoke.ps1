param(
    [Parameter(Mandatory = $true)]
    [string]$Token,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ServerId,
    [string]$GatewayId,
    [string]$DeviceId,
    [string]$ChannelId,
    [string]$CascadeId
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Join-ApiUrl {
    param(
        [string]$Path
    )
    $root = $BaseUrl.TrimEnd("/")
    if ($Path.StartsWith("/")) {
        return "$root$Path"
    }
    return "$root/$Path"
}

function Invoke-FlexbootApi {
    param(
        [ValidateSet("GET", "POST", "PUT", "DELETE")]
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $uri = Join-ApiUrl -Path $Path
    $headers = @{
        Authorization = "Bearer $Token"
    }

    if ($null -eq $Body) {
        $resp = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
    } else {
        $json = $Body | ConvertTo-Json -Depth 10
        $headers["Content-Type"] = "application/json"
        $resp = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -Body $json
    }

    if ($null -eq $resp) {
        throw "Empty response from $Method $Path"
    }
    if ($resp.code -ne 0) {
        throw "API failed: $Method $Path => code=$($resp.code), message=$($resp.message)"
    }
    return $resp.data
}

function New-SearchBody {
    return @{
        pageNumber = 1
        pageSize   = 20
        logic      = "AND"
        items      = @()
        orders     = @(@{ column = "createTime"; asc = $false })
    }
}

Write-Host "=== Media smoke start ==="

if ($ServerId) {
    Write-Host "[1] Testing media server: $ServerId"
    $test = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/server/test" -Body @{ serverId = $ServerId }
    Write-Host "    server test success=$($test.success), streamCount=$($test.streamCount), version=$($test.version)"
}

if ($GatewayId) {
    Write-Host "[2] Starting gateway: $GatewayId"
    $ok = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/gateway/$GatewayId/start"
    Write-Host "    gateway start result=$ok"
}

if ($DeviceId) {
    Write-Host "[3] Querying device detail: $DeviceId"
    $detail = Invoke-FlexbootApi -Method "GET" -Path "/api/admin/media/device/$DeviceId/detail"
    $channelCount = if ($null -eq $detail.channels) { 0 } else { $detail.channels.Count }
    Write-Host "    device=$($detail.device.deviceCode), channels=$channelCount"
}

if (-not $ChannelId) {
    Write-Host "[4] No ChannelId provided, trying to discover one from /api/admin/media/channel/list"
    $channels = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/list" -Body (New-SearchBody)
    if ($channels -and $channels.Count -gt 0) {
        $ChannelId = [string]$channels[0].id
        Write-Host "    discovered ChannelId=$ChannelId"
    }
}

if ($ChannelId) {
    Write-Host "[5] Starting live for channel: $ChannelId"
    $live = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/live" -Body @{
        channelId = $ChannelId
        protocol  = "http-flv"
    }
    Write-Host "    live session=$($live.sessionId), protocol=$($live.protocol)"

    if ($live.sessionId) {
        Write-Host "[6] Stopping live session: $($live.sessionId)"
        $stopLive = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/live/stop/$($live.sessionId)"
        Write-Host "    stop live result=$stopLive"
    }

    Write-Host "[7] Query playback records for last hour"
    $end = Get-Date
    $start = $end.AddHours(-1)
    $records = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/playback/query" -Body @{
        channelId = $ChannelId
        startTime = $start.ToString("yyyy-MM-dd HH:mm:ss")
        endTime   = $end.ToString("yyyy-MM-dd HH:mm:ss")
    }
    $recordCount = if ($null -eq $records) { 0 } else { $records.Count }
    Write-Host "    playback records=$recordCount"

    if ($recordCount -gt 0) {
        $first = $records[0]
        Write-Host "[8] Starting playback for first record: $($first.startTime) ~ $($first.endTime)"
        $play = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/playback/start" -Body @{
            channelId = $ChannelId
            startTime = $first.startTime
            endTime   = $first.endTime
            protocol  = "http-flv"
        }
        Write-Host "    playback session=$($play.sessionId)"
        if ($play.sessionId) {
            Write-Host "[9] Stopping playback session: $($play.sessionId)"
            $stopPlay = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/playback/stop/$($play.sessionId)"
            Write-Host "    stop playback result=$stopPlay"
        }
    }

    Write-Host "[10] PTZ test"
    $ptz = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/channel/ptz" -Body @{
        channelId = $ChannelId
        command   = "UP"
        speed     = 60
    }
    Write-Host "    ptz result=$ptz"
}

if ($CascadeId) {
    Write-Host "[11] Trigger cascade register: $CascadeId"
    $register = Invoke-FlexbootApi -Method "POST" -Path "/api/admin/media/cascade/$CascadeId/register"
    Write-Host "    cascade register trigger result=$register"
}

Write-Host "=== Media smoke completed ==="

