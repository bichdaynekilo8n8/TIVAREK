@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week9.ps1" -PromptDbCredentials -CleanBuild %*
pause
