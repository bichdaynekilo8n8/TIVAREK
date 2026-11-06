param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
& "$PSScriptRoot\smoke-week23-ui.ps1" -BaseUrl $BaseUrl
if ($LASTEXITCODE -ne 0) { exit 1 }
$failed = 0
function Check([bool]$ok, [string]$name) {
    if ($ok) { Write-Host "[PASS] $name" } else { $script:failed++; Write-Host "[FAIL] $name" }
}
foreach ($path in @('/', '/san-pham', '/sua-chua', '/gio-hang', '/thanh-toan', '/tra-cuu-sua-chua', '/tra-cuu-bao-hanh', '/api/csrf', '/login')) {
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + $path) -TimeoutSec 30
        Check ($res.StatusCode -eq 200) "GET $path"
        Check ([string]$res.Headers['Cache-Control'] -match 'no-store') "Private HTML/API cache: $path"
        if ($path -in @('/gio-hang', '/thanh-toan', '/tra-cuu-sua-chua', '/tra-cuu-bao-hanh', '/api/csrf', '/login')) {
            Check ([string]$res.Headers['X-Robots-Tag'] -match 'noindex') "Private SEO: $path"
        }
    } catch { $failed++; Write-Host "[FAIL] $path - $($_.Exception.Message)" }
}
foreach ($path in @('/css/week24.css', '/css/storefront.css')) {
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + $path) -TimeoutSec 30
        Check ([string]$res.Headers['Cache-Control'] -match 'max-age=3600') "Static cache: $path"
        Check ($res.Content -notmatch '@import') "No serial CSS imports: $path"
    } catch { $failed++; Write-Host "[FAIL] $path - $($_.Exception.Message)" }
}
try {
    $robots = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/robots.txt') -TimeoutSec 30
    Check ($robots.Content -match 'User-agent:') 'robots.txt'
    if ($robots.Content -match 'Sitemap:') {
        $map = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + '/sitemap.xml') -TimeoutSec 30
        $xml = [xml]$map.Content
        Check ($null -ne $xml.DocumentElement) 'Sitemap XML parses'
        Check ($map.Content -notmatch 'gio-hang|thanh-toan|tra-cuu|token=|localhost|127[.]0[.]0[.]1') 'Sitemap excludes private/DEV URLs'
    } else { Write-Host '[INFO] SEO disabled in DEV; sitemap intentionally unavailable.' }
} catch { $failed++; Write-Host "[FAIL] SEO - $($_.Exception.Message)" }
if ($failed -gt 0) { Write-Host "[FAILED] $failed check(s)."; exit 1 }
Write-Host '[PASS] Week 24 HTTP checks. Browser, SQL Server concurrency and UAT remain separate gates.'
exit 0
