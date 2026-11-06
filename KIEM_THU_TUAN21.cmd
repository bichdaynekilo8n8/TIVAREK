@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 21 - ALL TESTS (H2 PROFILE, NOT SQL PRODUCTION)
call "%~dp0mvnw.cmd" -B -ntp -Dspring.profiles.active=test clean verify
if errorlevel 1 (
  echo [TIVAREK] FAILURE - read the output and target\surefire-reports.
  pause
  exit /b 1
)
echo [TIVAREK] BUILD SUCCESS - review TUAN21_HOAN_CHINH.md for manual UAT.
pause
