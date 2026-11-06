@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 20 - 21 READ-ONLY HTTP PROBES
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\smoke-week19.ps1"
if errorlevel 1 (
    echo [TIVAREK] FAILURE. Check application log and database health.
    pause
    exit /b 1
)
echo [TIVAREK] HTTP smoke tests passed.
pause
