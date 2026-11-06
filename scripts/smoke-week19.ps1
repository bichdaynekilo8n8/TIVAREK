<#
.SYNOPSIS
  Read-only HTTP acceptance probes for TIVAREK week 19.
  Requires the application to be running. No login, no database writes.
.EXAMPLE
  .\scripts\smoke-week19.ps1 -BaseUrl 'http://127.0.0.1:8080'
#>
param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$origin = $BaseUrl.TrimEnd('/')
if ($origin -notmatch '^https?://[^/]+$') { throw 'BaseUrl must be an HTTP(S) origin without path.' }

function Get-Probe {
    param([string]$Path, [string]$Method = 'GET')
    $uri = "$origin$Path"
    try {
        $response = Invoke-WebRequest -Uri $uri -Method $Method -MaximumRedirection 0 -UseBasicParsing -TimeoutSec 20 -ErrorAction Stop
        return @{ Code = [int]$response.StatusCode; Body = [string]$response.Content; Location = [string]$response.Headers['Location'] }
    }
    catch {
        if (-not $_.Exception.Response) { throw "Network or application error at ${uri}: $_" }
        $response = $_.Exception.Response
        return @{ Code = [int]$response.StatusCode; Body = ''; Location = [string]$response.Headers['Location'] }
    }
}

$public = @(
    '/', '/san-pham', '/gio-hang', '/thanh-toan', '/sua-chua',
    '/tra-cuu-sua-chua', '/tra-cuu-bao-hanh', '/login',
    '/images/tivarek-icon.png', '/images/tivarek-wordmark.png'
)
$count = 0
foreach ($route in $public) {
    $result = Get-Probe -Path $route
    if ($result.Code -ne 200) { throw "FAIL: GET $route => $($result.Code) (expected 200)" }
    Write-Host "OK  GET $route => 200" -ForegroundColor Green
    $count++
    if ($route -eq '/') {
        if ($result.Body -notmatch '0335 135 549' -or $result.Body -notmatch 'tivarek@gmail.com') {
            throw 'FAIL: storefront is missing the configured public phone or email.'
        }
    }
}

$health = Get-Probe -Path '/actuator/health'
if ($health.Code -ne 200 -or $health.Body -notmatch '"status"\s*:\s*"UP"') {
    throw "FAIL: /actuator/health => $($health.Code); application is not UP."
}
Write-Host 'OK  GET /actuator/health => UP' -ForegroundColor Green
$count++

foreach ($route in @('/admin/stock', '/admin/orders', '/admin/repairs', '/admin/warranties', '/admin/reports', '/account/orders')) {
    $result = Get-Probe -Path $route
    if ($result.Code -ne 302 -or $result.Location -notmatch '/login') {
        throw "FAIL: guest GET $route => $($result.Code) (expected 302 to login)."
    }
    Write-Host "OK  guest GET $route => login redirect" -ForegroundColor Green
    $count++
}

# No CSRF token: Spring Security should reject before running any controller action.
foreach ($route in @('/gio-hang/add', '/sua-chua', '/tra-cuu-bao-hanh', '/bao-hanh/yeu-cau')) {
    $result = Get-Probe -Path $route -Method 'POST'
    if ($result.Code -ne 403) { throw "FAIL: POST $route without CSRF => $($result.Code) (expected 403)." }
    Write-Host "OK  POST $route without CSRF => 403" -ForegroundColor Green
    $count++
}
Write-Host "PASS: $count non-destructive HTTP checks." -ForegroundColor Green
