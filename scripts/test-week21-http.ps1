param([string]$BaseUrl = 'http://127.0.0.1:8080')

$ErrorActionPreference = 'Stop'
$ok = 0

function Assert-Page([string]$path, [string]$expected) {
    $url = $BaseUrl.TrimEnd('/') + $path
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 15
    if ($response.StatusCode -ne 200) { throw "Unexpected HTTP status at $path" }
    if ($response.Content -notmatch [regex]::Escape($expected)) { throw "Missing expected page element at $path" }
    if ($response.Headers['Referrer-Policy'] -ne 'no-referrer') { throw "Referrer-Policy missing at $path" }
    Write-Host "PASS $path"
    $script:ok++
}

# These probes only GET public pages; they do not modify production or DEV data.
Assert-Page '/login' '_csrf'
Assert-Page '/san-pham' 'TIVAREK'
Write-Host "[TIVAREK] $ok/$ok public security probes passed."
