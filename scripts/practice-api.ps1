param([string]$Username = "w9.sales", [string]$BaseUrl = "http://127.0.0.1:8080")
$ErrorActionPreference = "Stop"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$secret = Read-Host "Mat khau website cho $Username" -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    $csrf = Invoke-RestMethod "$BaseUrl/api/csrf" -WebSession $session
    $body = @{ username = $Username; password = $password }
    $body[$csrf.parameterName] = $csrf.token
    Invoke-WebRequest "$BaseUrl/login" -Method Post -Body $body -WebSession $session -UseBasicParsing | Out-Null
    Invoke-RestMethod "$BaseUrl/api/me" -WebSession $session | ConvertTo-Json
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $password = $null
    $body = $null
}
# Lay token moi sau dang nhap, van giu cung cookie jar.
$csrf = Invoke-RestMethod "$BaseUrl/api/csrf" -WebSession $session
$headers = @{}
$headers[$csrf.headerName] = $csrf.token
$customers = Invoke-RestMethod "$BaseUrl/api/admin/customers" -WebSession $session
$customers | Format-Table customerId,customerCode,fullName,phone
$code = "W9-" + [Guid]::NewGuid().ToString("N")
$json = @{ customerCode=$code; fullName="Khach thuc hanh tuan 9"; phone="0000000000"; email=$null } | ConvertTo-Json
$created = Invoke-RestMethod "$BaseUrl/api/admin/customers" -Method Post -WebSession $session -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($json))
$created | ConvertTo-Json
$json = @{ fullName="Khach da cap nhat tuan 9"; phone="0000000000"; email=$null } | ConvertTo-Json
Invoke-RestMethod "$BaseUrl/api/admin/customers/$($created.customerId)" -Method Put -WebSession $session -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([Text.Encoding]::UTF8.GetBytes($json)) | ConvertTo-Json
# GET /api/admin/costs bang w9.sales phai tra ve 403.
try {
    Invoke-RestMethod "$BaseUrl/api/admin/costs" -WebSession $session | ConvertTo-Json
} catch {
    Write-Host "Cost response: $($_.Exception.Response.StatusCode) (w9.sales: expected Forbidden/403)"
}
Invoke-WebRequest "$BaseUrl/logout" -Method Post -WebSession $session -Headers $headers -UseBasicParsing | Out-Null
Write-Host "Da dang xuat. Dung tai khoan w9.owner de xem nhat ky."
