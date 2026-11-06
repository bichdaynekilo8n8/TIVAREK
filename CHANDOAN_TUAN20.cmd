@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 20 - READ-ONLY ENVIRONMENT DIAGNOSTICS
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\diagnose-week20.ps1"
if errorlevel 1 (
    echo [TIVAREK] One or more prerequisites are unavailable.
    pause
    exit /b 1
)
echo [TIVAREK] Prerequisite checks passed.
pause
