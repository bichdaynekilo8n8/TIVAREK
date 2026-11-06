@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] TUAN 19 - KIEM THU JUNIT + H2 (KHONG CAN SQL SERVER)
call "%~dp0mvnw.cmd" -B -ntp clean verify
if errorlevel 1 (
  echo [TIVAREK] Tests hoac build loi. Kiem tra log phia tren.
  pause
  exit /b 1
)
echo [TIVAREK] PASS - Maven verify.
pause
