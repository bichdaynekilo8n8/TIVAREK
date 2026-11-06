@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\smoke-week18.ps1"
if errorlevel 1 (
  echo [TIVAREK] Smoke test that bai.
  pause
  exit /b 1
)
pause
