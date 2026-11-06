@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] TUAN 23 - JAVA 21 / FULL REGRESSION
call "%~dp0KIEM_THU_TUAN22.cmd"
if errorlevel 1 exit /b 1
echo [OK] Maven clean verify - kiem tra them giao dien bang smoke script khi app dang chay.
echo [NEXT] powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-week23-ui.ps1
exit /b 0
