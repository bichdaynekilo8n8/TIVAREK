@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] TUAN 19 - CHAY LOCAL (SQL SERVER DEV)
rem Ignore inherited PROD connection settings. Launcher always uses local training DB.
set "TIVAREK_DB_URL="
set "TIVAREK_DB_USER="
set "TIVAREK_DB_PASSWORD="
echo [TIVAREK] Nhap tai khoan SQL Server khi duoc hoi.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week9.ps1" -PromptDbCredentials -CleanBuild
if errorlevel 1 (
  echo [TIVAREK] Khong the khoi dong. Xem TUAN19_HOAN_CHINH.md.
  pause
  exit /b 1
)
pause
