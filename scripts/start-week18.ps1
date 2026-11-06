<#
.SYNOPSIS
  Start TIVAREK production from a built JAR. Never print database credentials.
.EXAMPLE
  .\scripts\start-week18.ps1 -Build
#>
param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java not found. Install JDK 21 and configure PATH.'
}
$javaVersion = (& cmd.exe /c 'java -version 2>&1' | Out-String)
if ($javaVersion -notmatch 'version "21(\.|\")|openjdk 21(\.|\s)') {
    throw 'Java 21 is required. Check java -version.'
}

foreach ($name in @('TIVAREK_DB_URL', 'TIVAREK_DB_USER', 'TIVAREK_DB_PASSWORD', 'TIVAREK_UPLOAD_DIR')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Missing environment variable: $name. See .env.example (not loaded automatically)."
    }
}
$uploadPath = [Environment]::GetEnvironmentVariable('TIVAREK_UPLOAD_DIR')
if (-not [IO.Path]::IsPathRooted($uploadPath)) {
    throw 'TIVAREK_UPLOAD_DIR must be an absolute path outside target.'
}
New-Item -ItemType Directory -Path $uploadPath -Force | Out-Null

if ($Build) {
    & .\mvnw.cmd '-B' '-ntp' '-Dspring.profiles.active=test' 'clean' 'verify'
    if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed. Production was not started.' }
}

$jar = Get-ChildItem -Path (Join-Path $projectRoot 'target') -Filter 'tivarek-*.jar' -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $jar) { throw 'Missing target/tivarek-*.jar. Run with -Build first.' }

Write-Host "TIVAREK production: $($jar.Name). Default bind: 127.0.0.1." -ForegroundColor Green
& java '-Dfile.encoding=UTF-8' '-jar' $jar.FullName '--spring.profiles.active=prod'
if ($LASTEXITCODE -ne 0) { throw "TIVAREK exited with code $LASTEXITCODE" }
