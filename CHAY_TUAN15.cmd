@echo off
setlocal
cd /d "%~dp0"
rem Khong chay mvnw.cmd truc tiep neu chua truyen mat khau SQL Server.
rem Script se hoi login va mat khau SQL Server, khong luu mat khau vao file.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week9.ps1" -PromptDbCredentials -CleanBuild %*
set "TIVAREK_RUN_EXIT=%ERRORLEVEL%"
if not "%TIVAREK_RUN_EXIT%"=="0" (
    echo.
    echo Neu SQL Server bao 18456, xem HUONG_DAN_SUA_SQL_TUAN15.md.
)
pause
exit /b %TIVAREK_RUN_EXIT%
