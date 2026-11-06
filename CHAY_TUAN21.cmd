@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 21 - LOCAL DEV ONLY (SQL SERVER DEV)
rem Reuse the existing, vetted Week 20 -> Week 19 local launch workflow.
call "%~dp0CHAY_TUAN20.cmd"
exit /b %ERRORLEVEL%
