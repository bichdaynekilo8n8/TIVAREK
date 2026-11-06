param([switch]$CreateDemoAccounts, [switch]$PromptDbCredentials, [switch]$CleanBuild)
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
if (-not (Test-Path ".\pom.xml")) { throw "Khong tim thay pom.xml trong project." }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw "Can cai Java 21 va dat JAVA_HOME/PATH." }
$javaVersion = (& cmd.exe /c 'java -version 2>&1' | Out-String)
if ($javaVersion -notmatch 'version "21(\.|\")|openjdk 21(\.|\s)') {
    throw 'TIVAREK requires JDK 21. Select Java 21 in PATH/JAVA_HOME, then retry.'
}
function Read-PasswordText([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}
if ([string]::IsNullOrWhiteSpace($env:TIVAREK_DB_URL)) {
    if ($env:DB_URL) { $env:TIVAREK_DB_URL = $env:DB_URL }
    else { $env:TIVAREK_DB_URL = "jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true" }
}
if ($CreateDemoAccounts -and $env:TIVAREK_DB_URL -notmatch '(?i)databaseName=TIVAREK_WEEK8_DEV(;|$)') {
    throw "Tao tai khoan mau chi dung DB TIVAREK_WEEK8_DEV. Dat TIVAREK_DB_URL ve DB thuc hanh Tuan 8."
}
if ($PromptDbCredentials) {
    $suggestedLogin = if ([string]::IsNullOrWhiteSpace($env:TIVAREK_DB_USER)) { "tivarek_dev" } else { $env:TIVAREK_DB_USER }
    $answer = Read-Host "SQL Server login [$suggestedLogin]"
    if ([string]::IsNullOrWhiteSpace($answer)) { $answer = $suggestedLogin }
    $env:TIVAREK_DB_USER = $answer
} elseif ([string]::IsNullOrWhiteSpace($env:TIVAREK_DB_USER)) {
    $answer = Read-Host "SQL Server login [tivarek_dev]"
    if ([string]::IsNullOrWhiteSpace($answer)) { $answer = "tivarek_dev" }
    $env:TIVAREK_DB_USER = $answer
}
$enteredDbPassword = $false
if ($PromptDbCredentials) {
    # Override any stale environment password; never print or write it to a file.
    $env:TIVAREK_DB_PASSWORD = Read-PasswordText "Mat khau SQL Server (khong phai mat khau website)"
    $enteredDbPassword = $true
} elseif ([string]::IsNullOrEmpty($env:TIVAREK_DB_PASSWORD)) {
    if ($env:DB_PASSWORD) { $env:TIVAREK_DB_PASSWORD = $env:DB_PASSWORD }
    else { $env:TIVAREK_DB_PASSWORD = Read-PasswordText "Mat khau SQL Server"; $enteredDbPassword = $true }
}
if ($CreateDemoAccounts) {
    $env:TIVAREK_DEMO_ENABLED = "true"
    $env:TIVAREK_DEMO_PASSWORD = Read-PasswordText "Mat khau cho 6 tai khoan website thuc hanh (>=12 ky tu, <=72 byte UTF-8)"
    if ($env:TIVAREK_DEMO_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:TIVAREK_DEMO_PASSWORD) -gt 72) {
        Remove-Item Env:TIVAREK_DEMO_PASSWORD -ErrorAction SilentlyContinue
        Remove-Item Env:TIVAREK_DEMO_ENABLED -ErrorAction SilentlyContinue
        throw "Mat khau website khong dung gioi han. Chay lai script."
    }
} else { Remove-Item Env:TIVAREK_DEMO_ENABLED -ErrorAction SilentlyContinue }
try {
    Write-Host "Website: http://127.0.0.1:8080/login"
    Write-Host "Dung Ctrl+C de dung ung dung. Mat khau khong duoc ghi vao file."
    if ($CleanBuild) {
        # Remove stale invalid Flyway filenames copied to target/classes by earlier builds.
        & ".\mvnw.cmd" clean spring-boot:run "-Dspring-boot.run.profiles=dev"
    } else {
        & ".\mvnw.cmd" spring-boot:run "-Dspring-boot.run.profiles=dev"
    }
    if ($LASTEXITCODE -ne 0) { throw "Ung dung chua khoi dong thanh cong. Xem loi phia tren." }
} finally {
    Remove-Item Env:TIVAREK_DEMO_ENABLED -ErrorAction SilentlyContinue
    Remove-Item Env:TIVAREK_DEMO_PASSWORD -ErrorAction SilentlyContinue
    if ($enteredDbPassword) { Remove-Item Env:TIVAREK_DB_PASSWORD -ErrorAction SilentlyContinue }
}
