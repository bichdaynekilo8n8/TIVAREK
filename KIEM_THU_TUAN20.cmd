@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 20 - BUILD + ALL JAVA TESTS (H2, NO SQL SERVER)
call "%~dp0mvnw.cmd" -B -ntp -Dspring.profiles.active=test clean verify
if errorlevel 1 (
    echo [TIVAREK] FAILURE. Review Maven output and target\surefire-reports.
    pause
    exit /b 1
)
echo [TIVAREK] SUCCESS. Run KIEM_THU_WEB_TUAN20.cmd after starting DEV.
pause
