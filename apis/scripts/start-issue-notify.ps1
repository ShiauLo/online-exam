[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "online_exam_db",
    [string]$DbUser = "root",
    [string]$DbPassword = "ChangeThisMySQLPassword_2026",
    [string]$NacosAddr = "127.0.0.1:8848",
    [string]$NacosUser = "nacos",
    [string]$NacosPassword = "ChangeThisNacosPassword_2026",
    [string]$JwtSecret = "mySuperSecretKeyThatIsAtLeast32BytesLongForHS512",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
$moduleDir = Join-Path $repoRoot "exam-issue-notify"
$entryFile = Join-Path $moduleDir "dist/src/server.js"
$logDir = Join-Path $repoRoot ".runtime-logs"
$outLog = Join-Path $logDir "exam-issue-notify.out.log"
$errLog = Join-Path $logDir "exam-issue-notify.err.log"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

$existing = Get-NetTCPConnection -LocalPort 8091 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existing) {
    $process = Get-Process -Id $existing.OwningProcess -ErrorAction SilentlyContinue
    throw "8091 端口已被占用，当前进程: $($process.ProcessName)($($existing.OwningProcess))。请先手工停止旧进程后再启动。"
}

if ($Rebuild -or -not (Test-Path $entryFile)) {
    Write-Section "构建 issue-notify"
    Push-Location $moduleDir
    try {
        npm run build
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $entryFile)) {
    throw "未找到可启动入口: $entryFile"
}

Write-Section "启动 issue-notify"
$env:EXAM_DB_HOST = $DbHost
$env:EXAM_DB_PORT = "$DbPort"
$env:EXAM_DB_NAME = $DbName
$env:EXAM_DB_USERNAME = $DbUser
$env:EXAM_DB_PASSWORD = $DbPassword
$env:EXAM_NACOS_ENABLED = "true"
$env:EXAM_NACOS_ADDR = $NacosAddr
$env:EXAM_NACOS_USERNAME = $NacosUser
$env:EXAM_NACOS_PASSWORD = $NacosPassword
$env:EXAM_NACOS_NAMESPACE = "public"
$env:EXAM_NACOS_GROUP = "DEFAULT_GROUP"
$env:EXAM_ISSUE_NOTIFY_IP = "127.0.0.1"
$env:EXAM_ISSUE_NOTIFY_PORT = "8091"
$env:EXAM_JWT_SECRET = $JwtSecret

$process = Start-Process -FilePath "D:\Develop\node.js\node.exe" `
    -ArgumentList $entryFile `
    -WorkingDirectory $moduleDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 8
$listening = Get-NetTCPConnection -LocalPort 8091 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $listening) {
    Write-Section "启动失败日志"
    if (Test-Path $outLog) {
        Get-Content $outLog -Tail 60
    }
    if (Test-Path $errLog) {
        Get-Content $errLog -Tail 60
    }
    throw "exam-issue-notify 未成功监听 8091"
}

Write-Section "启动完成"
Write-Host "PID: $($process.Id)"
Write-Host "日志: $outLog"
Write-Host "端口: 8091"