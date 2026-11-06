@echo off
cd /d "%~dp0"
call .\mvnw.cmd clean test
pause
