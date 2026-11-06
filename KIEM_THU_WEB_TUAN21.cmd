@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 21 - READ-ONLY HTTP SECURITY PROBES
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\test-week21-http.ps1"
if errorlevel 1 (
  echo [TIVAREK] HTTP PROBES FAILED.
  pause
  exit /b 1
)
echo [TIVAREK] HTTP PROBES PASS. Also run KIEM_THU_WEB_TUAN20.cmd.
pause
