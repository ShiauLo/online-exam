[CmdletBinding()]
param(
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "online_exam_db",
    [string]$DbUser = "root",
    [string]$DbPassword = "ChangeThisMySQLPassword_2026",
    [string]$RedisHost = "127.0.0.1",
    [int]$RedisPort = 6379,
    [int]$RedisDatabase = 0,
    [string]$RedisPassword = "ChangeThisRedisPassword_2026",
    [string]$NacosAddr = "127.0.0.1:8848",
    [string]$NacosUser = "nacos",
    [string]$NacosPassword = "ChangeThisNacosPassword_2026",
    [string]$ExamCoreBaseUrl = "http://127.0.0.1:8086",
    [string]$JwtSecret = "mySuperSecretKeyThatIsAtLeast32BytesLongForHS512",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
$moduleDir = Join-Path $repoRoot "exam-realtime"
$entryFile = Join-Path $moduleDir "dist/src/server.js"
$logDir = Join-Path $repoRoot ".runtime-logs"
$outLog = Join-Path $logDir "exam-realtime.out.log"
$errLog = Join-Path $logDir "exam-realtime.err.log"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

$existing = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existing) {
    $process = Get-Process -Id $existing.OwningProcess -ErrorAction SilentlyContinue
    throw "8090 端口已被占用，当前进程: $($process.ProcessName)($($existing.OwningProcess))。请先手工停止旧进程后再启动。"
}

if ($Rebuild -or -not (Test-Path $entryFile)) {
    Write-Section "构建 realtime"
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

Write-Section "启动 realtime"
$env:EXAM_DB_HOST = $DbHost
$env:EXAM_DB_PORT = "$DbPort"
$env:EXAM_DB_NAME = $DbName
$env:EXAM_DB_USERNAME = $DbUser
$env:EXAM_DB_PASSWORD = $DbPassword
$env:EXAM_REDIS_HOST = $RedisHost
$env:EXAM_REDIS_PORT = "$RedisPort"
$env:EXAM_REDIS_DATABASE = "$RedisDatabase"
$env:EXAM_REDIS_PASSWORD = $RedisPassword
$env:EXAM_NACOS_ADDR = $NacosAddr
$env:EXAM_NACOS_USERNAME = $NacosUser
$env:EXAM_NACOS_PASSWORD = $NacosPassword
$env:EXAM_REALTIME_IP = "127.0.0.1"
$env:EXAM_REALTIME_PORT = "8090"
$env:EXAM_REALTIME_EXAM_CORE_BASE_URL = $ExamCoreBaseUrl
$env:EXAM_JWT_SECRET = $JwtSecret

$process = Start-Process -FilePath "D:\Develop\node.js\node.exe" `
    -ArgumentList $entryFile `
    -WorkingDirectory $moduleDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 8
$listening = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $listening) {
    Write-Section "启动失败日志"
    if (Test-Path $outLog) {
        Get-Content $outLog -Tail 60
    }
    if (Test-Path $errLog) {
        Get-Content $errLog -Tail 60
    }
    throw "exam-realtime 未成功监听 8090"
}

Write-Section "启动完成"
Write-Host "PID: $($process.Id)"
Write-Host "日志: $outLog"
Write-Host "端口: 8090"