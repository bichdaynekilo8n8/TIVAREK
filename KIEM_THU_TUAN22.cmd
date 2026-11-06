@echo off
setlocal
cd /d "%~dp0"
echo [TIVAREK] WEEK 22 - JAVA 21 / H2 REGRESSION TESTS
java -version
if errorlevel 1 (
  echo [ERROR] Khong tim thay Java. Hay cai JDK 21.
  exit /b 1
)
powershell.exe -NoProfile -Command "$v = (java -version 2>&1 | Out-String); if ($v -notmatch '21[.]') { exit 1 }"
if errorlevel 1 (
  echo [ERROR] Can JDK 21 de compile ma nguon TIVAREK.
  exit /b 1
)
call "%~dp0mvnw.cmd" -B -ntp -Dspring.profiles.active=test clean verify
if errorlevel 1 (
  echo [ERROR] Maven verify that bai. Xem log de sua loi truoc khi chay website.
  exit /b 1
)
echo [OK] Unit/integration tests tren H2 da hoan tat.
echo [NEXT] Chay KIEM_THU_TUAN22_SQLSERVER.cmd tren database DEV rieng.
exit /b 0
