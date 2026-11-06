@echo off
cd /d "%~dp0"
call ".\mvnw.cmd" "-Dtest=StorefrontIntegrationTest" test
if errorlevel 1 (
    echo KIEM THU TUAN 13 CHUA THANH CONG - xem loi phia tren.
) else (
    echo KIEM THU TUAN 13 THANH CONG.
)
pause
