param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$checks = @(
    @{ Path = '/'; Marker = 'w23-hero' },
    @{ Path = '/san-pham'; Marker = 'sf-shop-layout' },
    @{ Path = '/sua-chua'; Marker = 'w23-repair-grid' },
    @{ Path = '/tra-cuu-sua-chua'; Marker = 'w23-tracking-layout' },
    @{ Path = '/gio-hang'; Marker = 'w23-order-main' },
    @{ Path = '/thanh-toan'; Marker = 'w23-checkout-layout' },
    @{ Path = '/css/week23.css'; Marker = 'w23-home' },
    @{ Path = '/js/week23-forms.js'; Marker = 'data-w23-fulfillment' }
)
$failed = 0
foreach ($test in $checks) {
    try {
        $res = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl.TrimEnd('/') + $test.Path) -TimeoutSec 15 -MaximumRedirection 3
        $ok = $res.StatusCode -eq 200 -and $res.Content.Contains($test.Marker)
        if ($ok) {
            Write-Host ('[PASS] GET ' + $test.Path)
        } else {
            $failed++
            Write-Host ('[FAIL] GET ' + $test.Path + ' - status/content mismatch')
        }
    } catch {
        $failed++
        Write-Host ('[FAIL] GET ' + $test.Path + ' - ' + $_.Exception.Message)
    }
}
if ($failed -gt 0) { Write-Host ('[FAILED] ' + $failed + ' check(s).'); exit 1 }
Write-Host '[PASS] 8 giao dien/tai nguyen da hoat dong.'
exit 0
