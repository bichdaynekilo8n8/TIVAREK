<#
.SYNOPSIS
  Read-only local diagnostics. Does not connect to SQL Server with credentials.
.EXAMPLE
  .\scripts\diagnose-week20.ps1
#>
param([string]$DbHost = '127.0.0.1', [int]$DbPort = 1433)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$failed = $false

function Check-Result([bool]$Ok,[string]$Description) {
    if ($Ok) { Write-Host "PASS $Description" -ForegroundColor Green }
    else { Write-Host "FAIL $Description" -ForegroundColor Red; $script:failed = $true }
}

Check-Result (Test-Path -LiteralPath (Join-Path $projectRoot 'pom.xml') -PathType Leaf) 'pom.xml is present'
Check-Result (Test-Path -LiteralPath (Join-Path $projectRoot 'mvnw.cmd') -PathType Leaf) 'Maven wrapper is present'

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $javaCmd) {
    Check-Result $false 'JDK 21 is available on PATH'
} else {
    $detected = (& cmd.exe /c 'java -version 2>&1' | Out-String)
    Check-Result ([bool]($detected -match 'version "21(\.|\")|openjdk 21(\.|\s)')) 'JDK 21 is selected on PATH'
}

$client = $null
try {
    if ($DbPort -lt 1 -or $DbPort -gt 65535) { throw 'DbPort must be in 1..65535.' }
    $client = [System.Net.Sockets.TcpClient]::new()
    $attempt = $client.BeginConnect($DbHost,$DbPort,$null,$null)
    $connected = $attempt.AsyncWaitHandle.WaitOne(3000,$false)
    if ($connected) { $client.EndConnect($attempt) }
    Check-Result $connected "SQL Server TCP listener $DbHost`:$DbPort is reachable"
} catch {
    Check-Result $false "SQL Server TCP listener $DbHost`:$DbPort is reachable"
} finally {
    if ($null -ne $client) { $client.Dispose() }
}

if ([string]::IsNullOrWhiteSpace($env:TIVAREK_DB_URL)) {
    Write-Host 'INFO: TIVAREK_DB_URL is not set. CHAY_TUAN20.cmd uses the local DEV database by design.' -ForegroundColor Cyan
} else {
    Write-Host 'INFO: A custom database URL is set in this shell. Check its target before manual deployment.' -ForegroundColor Yellow
}
Write-Host 'INFO: No SQL password, user name, or personal data is printed.' -ForegroundColor Cyan

if ($failed) { exit 1 }
exit 0
