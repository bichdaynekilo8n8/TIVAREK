@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] TUAN 23 - GIAO DIEN RESPONSIVE / SQL SERVER DEV
echo [INFO] Backend khong doi DB hay Flyway so voi Tuan 22.
call "%~dp0CHAY_TUAN22.cmd"
exit /b %ERRORLEVEL%
