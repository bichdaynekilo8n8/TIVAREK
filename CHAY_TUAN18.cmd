@echo off
setlocal
cd /d "%~dp0"
echo ================================================
echo TIVAREK WEEK 18 - LOCAL MODE (SQL Server DEV)
echo ================================================
echo Login and SQL Server password will be requested.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week9.ps1" -PromptDbCredentials -CleanBuild
if errorlevel 1 goto failed
goto end
:failed
echo [TIVAREK] Local startup failed. Check SQL Server credentials and server availability.
pause
exit /b 1
:end
endlocal
