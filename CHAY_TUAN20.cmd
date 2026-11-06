@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 20 - LOCAL DEV ONLY
rem Week 19 local launcher clears inherited DB variables and prompts for DEV credentials.
call "%~dp0CHAY_TUAN19.cmd"
exit /b %ERRORLEVEL%
