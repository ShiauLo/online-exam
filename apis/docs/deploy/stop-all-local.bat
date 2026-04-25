@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_PATH=%SCRIPT_DIR%stop-all-local.ps1"

where pwsh >nul 2>nul
if not errorlevel 1 (
  pwsh -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_PATH%" %*
  exit /b %ERRORLEVEL%
)

powershell -NoProfile -ExecutionPolicy Bypass -Command "& ([ScriptBlock]::Create((Get-Content -Raw -Encoding UTF8 '%SCRIPT_PATH%'))) %*"
exit /b %ERRORLEVEL%
