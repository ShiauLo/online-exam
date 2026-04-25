@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "SSH_HOST=root@47.105.121.232"
set "PORTS=3306 6379 5672 8848 9848 9849 15672"
set "PORTS_PS=3306,6379,5672,8848,9848,9849,15672"
set "TUNNEL_STATE=FREE"
set "SSH_READY_COUNT=0"
set "PORT_COUNT=0"
set "BLOCKED_PORTS="

call :check_ports
if /i "!TUNNEL_STATE!"=="READY" goto already_ready
if /i "!TUNNEL_STATE!"=="BLOCKED" goto blocked

echo Starting SSH tunnel...
echo Enter your SSH password in this window if prompted.
echo After the tunnel is ready, this window will show a success message.
echo Press Ctrl+C in this window when you want to close the tunnel.
echo.

ssh -tt -o ExitOnForwardFailure=yes -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -L 3306:127.0.0.1:3306 -L 6379:127.0.0.1:6379 -L 5672:127.0.0.1:5672 -L 8848:127.0.0.1:8848 -L 9848:127.0.0.1:9848 -L 9849:127.0.0.1:9849 -L 15672:127.0.0.1:15672 %SSH_HOST% "printf '\n[OK] SSH tunnel is ready.\nLocal ports: 3306 6379 5672 8848 9848 9849 15672\nKeep this window open while you use the services.\nPress Ctrl+C to close the tunnel.\n\n'; while true; do sleep 3600; done"
if errorlevel 1 goto failed
goto closed

:check_ports
for /f "usebackq tokens=1,2,3 delims=|" %%A in (`powershell -NoProfile -Command "$ports=@(%PORTS_PS%); foreach($port in $ports){ $conn=Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1; if($conn){ $proc=Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue; $name=if($proc){$proc.ProcessName}else{'unknown'}; Write-Output ('{0}|{1}|{2}' -f $port,$conn.OwningProcess,$name) } else { Write-Output ('{0}||' -f $port) } }"`) do (
  if not "%%B"=="" (
    if /i "%%C"=="ssh" (
      set /a SSH_READY_COUNT+=1
    ) else (
      if defined BLOCKED_PORTS (
        set "BLOCKED_PORTS=!BLOCKED_PORTS!, %%A(%%C PID=%%B)"
      ) else (
        set "BLOCKED_PORTS=%%A(%%C PID=%%B)"
      )
    )
  )
)

if defined BLOCKED_PORTS (
  set "TUNNEL_STATE=BLOCKED"
  exit /b 0
)

for %%P in (%PORTS%) do set /a PORT_COUNT+=1
if "!SSH_READY_COUNT!"=="!PORT_COUNT!" set "TUNNEL_STATE=READY"
exit /b 0

:already_ready
echo SSH tunnel is already ready.
echo Local ports: %PORTS%
echo You can use the services now.
echo.
ping 127.0.0.1 -n 4 >nul
exit /b 0

:blocked
echo SSH tunnel was not started because some local ports are already occupied by non-SSH processes.
echo These ports are not an existing tunnel:
echo !BLOCKED_PORTS!
echo.
echo Please stop the conflicting local services first, then run this script again.
echo If you only want to use local MySQL/Redis/Nacos, you do not need this tunnel.
echo.
pause
exit /b 1

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
