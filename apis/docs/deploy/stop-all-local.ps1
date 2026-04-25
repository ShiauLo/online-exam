[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$logDir = Join-Path $repoRoot ".runtime-logs"
$pidFile = Join-Path $logDir "local-dev-stack.processes.json"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

if (-not (Test-Path $pidFile)) {
    throw "未找到进程记录文件：$pidFile。请先运行 start-all-local.ps1，或手工停止现有服务。"
}

$processes = Get-Content $pidFile -Raw | ConvertFrom-Json
if ($processes -isnot [System.Array]) {
    $processes = @($processes)
}

Write-Section "停止本地联调栈"
foreach ($item in ($processes | Sort-Object -Property port -Descending)) {
    $process = Get-Process -Id $item.pid -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $item.pid -Force
        Write-Host "$($item.name) 已停止，PID=$($item.pid)"
    } else {
        Write-Host "$($item.name) 进程不存在，PID=$($item.pid)"
    }
}

Remove-Item $pidFile -Force
Write-Host ""
Write-Host "已清理进程记录：$pidFile"
