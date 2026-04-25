[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

throw "exam-issue-notify 已并入 exam-realtime，不再作为独立进程启动。请改用 scripts/start-realtime.ps1，并让前端问题通知连接到 http://127.0.0.1:8090 path=/issue-socket.io。"
