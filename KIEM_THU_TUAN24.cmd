@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] TUAN 24 - JAVA 21 / FULL REGRESSION
call "%~dp0KIEM_THU_TUAN23.cmd"
if errorlevel 1 exit /b 1
echo [NEXT] Start SQL Server DEV and CHAY_TUAN24.cmd, then run:
echo powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-week24-ui.ps1
exit /b 0
