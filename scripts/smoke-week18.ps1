param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$origin = $BaseUrl.TrimEnd('/')
foreach ($route in @('/actuator/health', '/', '/san-pham', '/login', '/images/tivarek-icon.png', '/images/tivarek-wordmark.png')) {
    $uri = "$origin$route"
    try {
        $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 20
        if ($response.StatusCode -ne 200) { throw "HTTP $($response.StatusCode)" }
        if ($route -eq '/actuator/health' -and $response.Content -notmatch '"status"\s*:\s*"UP"') { throw 'Health status is not UP.' }
        if ($route -eq '/' -and ($response.Content -notmatch '0335 135 549' -or $response.Content -notmatch 'tivarek@gmail.com')) { throw 'Contact details missing.' }
        Write-Host "OK 200 $route" -ForegroundColor Green
    }
    catch { throw "Smoke test failed at ${uri}: $_" }
}
Write-Host 'PASS: all six endpoints returned expected results.' -ForegroundColor Green
