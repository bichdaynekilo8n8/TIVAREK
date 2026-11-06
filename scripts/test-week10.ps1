param([switch]$SqlServer)
$ErrorActionPreference="Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)
Remove-Item Env:TIVAREK_DEMO_ENABLED -ErrorAction SilentlyContinue
$enteredDbPassword=$false
try {
    if ($SqlServer) {
        if (-not $env:TIVAREK_DB_URL) {
            if ($env:DB_URL) { $env:TIVAREK_DB_URL=$env:DB_URL }
            else { $env:TIVAREK_DB_URL="jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true" }
        }
        if ($env:TIVAREK_DB_URL -notmatch '(?i)databaseName=TIVAREK_WEEK8_DEV(;|$)') {
            throw "Kiem thu SQL Server chi chay tren DB thuc hanh TIVAREK_WEEK8_DEV."
        }
        if (-not $env:TIVAREK_DB_USER) {
            $answer=Read-Host "SQL Server login [tivarek_dev]"
            if ([string]::IsNullOrWhiteSpace($answer)) { $answer="tivarek_dev" }
            $env:TIVAREK_DB_USER=$answer
        }
        if (-not $env:TIVAREK_DB_PASSWORD) {
            if ($env:DB_PASSWORD) { $env:TIVAREK_DB_PASSWORD=$env:DB_PASSWORD }
            else {
                $secure=Read-Host "Mat khau SQL Server" -AsSecureString
                $pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
                try { $env:TIVAREK_DB_PASSWORD=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer); $enteredDbPassword=$true }
                finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
            }
        }
        $env:RUN_SQLSERVER_TESTS="true"
        & ".\mvnw.cmd" "-Dtest=SqlServerSecurityTest,SqlServerProductTest" test
    } else {
        Remove-Item Env:RUN_SQLSERVER_TESTS -ErrorAction SilentlyContinue
        & ".\mvnw.cmd" test
    }
    if ($LASTEXITCODE -ne 0) { throw "Kiem thu chua thanh cong. Xem ket qua phia tren." }
} finally {
    Remove-Item Env:RUN_SQLSERVER_TESTS -ErrorAction SilentlyContinue
    if ($enteredDbPassword) { Remove-Item Env:TIVAREK_DB_PASSWORD -ErrorAction SilentlyContinue }
}
