@echo off
setlocal
cd /d "%~dp0"
echo ==================================================
echo TIVAREK - TUAN 17 - TEST VA CHAY SPRING BOOT
echo ==================================================
java -version
if errorlevel 1 goto failed
call mvnw.cmd clean test
if errorlevel 1 goto failed
echo.
echo Tests thanh cong. Dang khoi dong ung dung va yeu cau SQL Server credentials...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-week9.ps1" -PromptDbCredentials -CleanBuild
if errorlevel 1 goto failed
goto end
:failed
echo.
echo Co loi khi kiem tra hoac khoi dong. Xem log phia tren.
pause
:end
endlocal
