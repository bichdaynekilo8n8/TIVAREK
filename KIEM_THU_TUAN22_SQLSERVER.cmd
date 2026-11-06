@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 22 - REAL SQL SERVER TESTS (DEV ONLY)
echo [IMPORTANT] Chi duoc chay voi database TIVAREK_WEEK8_DEV da sao luu.
echo [IMPORTANT] Khong chay voi production hoac database chua sao luu.
rem Neu URL do nguoi dung cung cap, kiem tra DB name TRUOC KHI Spring Flyway khoi dong.
if not defined TIVAREK_DB_URL if defined DB_URL set "TIVAREK_DB_URL=%DB_URL%"
if defined TIVAREK_DB_URL (
  powershell.exe -NoProfile -Command "$url = $env:TIVAREK_DB_URL; if ($url -notmatch '(?i)(^|;)databaseName=TIVAREK_WEEK8_DEV(;|$)') { exit 1 }"
  if errorlevel 1 (
    echo [ERROR] DB URL khong tro den TIVAREK_WEEK8_DEV. Da dung truoc khi migrate.
    exit /b 1
  )
) else (
  set "TIVAREK_DB_URL=jdbc:sqlserver://127.0.0.1:1433;databaseName=TIVAREK_WEEK8_DEV;encrypt=true;trustServerCertificate=true"
)
set "DB_URL="
set RUN_SQLSERVER_TESTS=true
call "%~dp0mvnw.cmd" -B -ntp -Dspring.profiles.active=dev -Dtest=Week22SqlServerConcurrencyTest test
exit /b %ERRORLEVEL%
