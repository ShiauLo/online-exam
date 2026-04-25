@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "SSH_HOST=root@47.105.121.232"
set "PORTS=3306 6379 5672 8848 9848 9849 15672"

call :check_ports
if "!ALL_READY!"=="1" goto already_ready

echo Starting SSH tunnel...
echo Enter your SSH password in this window if prompted.
echo After the tunnel is ready, this window will show a success message.
echo Press Ctrl+C in this window when you want to close the tunnel.
echo.

ssh -tt -o ExitOnForwardFailure=yes -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -L 3306:127.0.0.1:3306 -L 6379:127.0.0.1:6379 -L 5672:127.0.0.1:5672 -L 8848:127.0.0.1:8848 -L 9848:127.0.0.1:9848 -L 9849:127.0.0.1:9849 -L 15672:127.0.0.1:15672 %SSH_HOST% "printf '\n[OK] SSH tunnel is ready.\nLocal ports: 3306 6379 5672 8848 9848 9849 15672\nKeep this window open while you use the services.\nPress Ctrl+C to close the tunnel.\n\n'; while true; do sleep 3600; done"
if errorlevel 1 goto failed
goto closed

:check_ports
set "ALL_READY=1"
for %%P in (%PORTS%) do (
  netstat -ano | findstr /r /c:":%%P .*LISTENING" >nul
  if errorlevel 1 set "ALL_READY=0"
)
exit /b 0

:already_ready
echo SSH tunnel is already ready.
echo Local ports: %PORTS%
echo You can use the services now.
echo.
ping 127.0.0.1 -n 4 >nul
exit /b 0

:closed
echo SSH tunnel has been closed.
echo.
pause
exit /b 0

:failed
echo SSH tunnel failed to start or was interrupted.
echo Please check:
echo 1. Whether the SSH password was correct.
echo 2. Whether ssh is installed and available in PATH.
echo 3. Whether any local port in %PORTS% is already in use.
echo.
pause
exit /b 1
