[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

throw "exam-issue-core 已并入 exam-core，不再作为独立进程启动。请改用聚合宿主 exam-core。若需本地一键拉起当前全部服务，请执行 docs/deploy/start-all-local.ps1；若只做服务器侧统一启动，请执行 scripts/deploy/start-all-server.sh。"
