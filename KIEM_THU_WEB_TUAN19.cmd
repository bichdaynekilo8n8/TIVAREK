@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] TUAN 19 - SMOKE TEST WEB DANG CHAY O 127.0.0.1:8080
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\smoke-week19.ps1"
if errorlevel 1 (
  echo [TIVAREK] Co endpoint khong dung. Xem log ung dung.
  pause
  exit /b 1
)
echo [TIVAREK] PASS - HTTP smoke test.
pause
