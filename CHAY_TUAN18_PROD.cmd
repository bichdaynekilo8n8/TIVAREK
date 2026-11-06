@echo off
setlocal
cd /d "%~dp0"
echo ================================================
echo TIVAREK WEEK 18 - PRODUCTION MODE
echo ================================================
echo Requires configured TIVAREK_DB_URL, TIVAREK_DB_USER,
echo TIVAREK_DB_PASSWORD and TIVAREK_UPLOAD_DIR.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week18.ps1" -Build
if errorlevel 1 goto failed
goto end
:failed
echo [TIVAREK] Production startup failed. See TUAN18_HOAN_CHINH.md.
pause
exit /b 1
:end
endlocal
