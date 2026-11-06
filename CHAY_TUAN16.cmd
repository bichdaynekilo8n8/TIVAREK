@echo off
setlocal
cd /d "%~dp0"
rem Java 21 + SQL Server; nhap mat khau moi lan, khong luu vao ma nguon.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week9.ps1" -PromptDbCredentials -CleanBuild %*
set "TIVAREK_RUN_EXIT=%ERRORLEVEL%"
if not "%TIVAREK_RUN_EXIT%"=="0" (
    echo.
    echo Kiem tra ket noi SQL Server va HUONG_DAN_SUA_SQL_TUAN15.md.
)
pause
exit /b %TIVAREK_RUN_EXIT%
