@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 22 - LOCAL DEVELOPMENT (SQL SERVER DEV)
rem Bao ve khoi env DB_URL cu co the tro toi production.
set "DB_URL="
set "DB_USERNAME="
set "DB_PASSWORD="
rem Ke thua cau hinh va khoi dong DEV da su dung tu tuan 21.
call "%~dp0CHAY_TUAN21.cmd"
exit /b %ERRORLEVEL%
