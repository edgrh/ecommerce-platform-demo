@echo off
setlocal
cd /d "%~dp0"
echo [INFO] Starting one-click launcher...
powershell -ExecutionPolicy Bypass -NoProfile -File ".\scripts\one-click.ps1"
set EXITCODE=%ERRORLEVEL%
if not "%EXITCODE%"=="0" (
  echo.
  echo [ERROR] one-click script exited with code %EXITCODE%.
  pause
  exit /b %EXITCODE%
)
echo.
echo [INFO] one-click script finished.
pause
exit /b 0
