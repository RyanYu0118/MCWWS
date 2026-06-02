@echo off
setlocal

cd /d "%~dp0\.."

echo [fix-bluemap-web] repo: %cd%
powershell -NoProfile -ExecutionPolicy Bypass -File "%cd%\scripts\fix-bluemap-web.ps1"

echo.
echo [fix-bluemap-web] press any key to close...
pause >nul

