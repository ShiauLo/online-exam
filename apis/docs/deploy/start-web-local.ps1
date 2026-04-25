[CmdletBinding()]
param(
    [string]$ApiBaseUrl = "http://127.0.0.1:8080",
    [string]$WsBaseUrl = "http://127.0.0.1:8090",
    [string]$IssueWsBaseUrl = "http://127.0.0.1:8091",
    [string]$UseMock = "false",
    [int]$Port = 5173,
    [int]$WaitSeconds = 20
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$webDir = Join-Path $repoRoot "web"
$logDir = Join-Path $repoRoot ".runtime-logs"
$outLog = Join-Path $logDir "web-local.out.log"
$errLog = Join-Path $logDir "web-local.err.log"
$pidFile = Join-Path $logDir "web-local.pid"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Resolve-Executable {
    param(
        [string]$CommandName,
        [string[]]$FallbackPaths = @()
    )

    $resolved = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved) {
        return $resolved.Source
    }

    foreach ($path in $FallbackPaths) {
        if (Test-Path $path) {
            return $path
        }
    }

    throw "未找到可执行命令 $CommandName，请先安装或把它加入 PATH。"
}

function Wait-PortListening {
    param(
        [int]$TargetPort,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Get-NetTCPConnection -LocalPort $TargetPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1) {
            return $true
        }
        Start-Sleep -Seconds 1
    }

    return $false
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

$existing = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existing) {
    $process = Get-Process -Id $existing.OwningProcess -ErrorAction SilentlyContinue
    throw "前端端口 $Port 已被 $($process.ProcessName)($($existing.OwningProcess)) 占用，请先停止旧进程。"
}

$nodeExe = Resolve-Executable -CommandName "node.exe" -FallbackPaths @(
    "D:\Develop\node.js\node.exe",
    "C:\Program Files\nodejs\node.exe"
)
$npmCmd = Resolve-Executable -CommandName "npm.cmd" -FallbackPaths @(
    "D:\Develop\node.js\npm.cmd",
    "C:\Program Files\nodejs\npm.cmd"
)

if (-not (Test-Path (Join-Path $webDir "node_modules"))) {
    Write-Section "安装 web 依赖"
    Push-Location $webDir
    try {
        & $npmCmd install
        if ($LASTEXITCODE -ne 0) {
            throw "web npm install 失败。"
        }
    } finally {
        Pop-Location
    }
}

$viteEntry = Join-Path $webDir "node_modules/vite/bin/vite.js"
if (-not (Test-Path $viteEntry)) {
    throw "未找到 Vite 启动入口：$viteEntry"
}

if (Test-Path $outLog) {
    Remove-Item $outLog -Force
}
if (Test-Path $errLog) {
    Remove-Item $errLog -Force
}

Write-Section "启动本地前端"
$env:VITE_API_BASE_URL = $ApiBaseUrl
$env:VITE_WS_BASE_URL = $WsBaseUrl
$env:VITE_ISSUE_WS_BASE_URL = $IssueWsBaseUrl
$env:VITE_USE_MOCK = $UseMock

$process = Start-Process -FilePath $nodeExe `
    -ArgumentList @($viteEntry, "--host", "127.0.0.1", "--port", "$Port", "--strictPort") `
    -WorkingDirectory $webDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

if (-not (Wait-PortListening -TargetPort $Port -TimeoutSeconds $WaitSeconds)) {
    if (Test-Path $outLog) {
        Get-Content $outLog -Tail 80
    }
    if (Test-Path $errLog) {
        Get-Content $errLog -Tail 80
    }
    throw "web 未在 ${WaitSeconds}s 内监听端口 $Port。"
}

$process.Id | Set-Content -Path $pidFile -Encoding ASCII
Write-Host "前端已启动，PID=$($process.Id)"
Write-Host "访问地址: http://127.0.0.1:$Port"
Write-Host "当前后端地址: $ApiBaseUrl"
