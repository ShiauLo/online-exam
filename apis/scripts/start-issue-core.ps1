[CmdletBinding()]
param(
    [string]$NacosAddr = "127.0.0.1:8848",
    [string]$NacosUser = "nacos",
    [string]$NacosPassword = "ChangeThisNacosPassword_2026",
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "online_exam_db",
    [string]$DbUser = "root",
    [string]$DbPassword = "ChangeThisMySQLPassword_2026",
    [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path $PSScriptRoot -Parent
$moduleDir = Join-Path $repoRoot "exam-issue-core"
$targetJar = Join-Path $moduleDir "target/exam-issue-core-0.0.1-SNAPSHOT.jar"
$logDir = Join-Path $repoRoot ".runtime-logs"
$outLog = Join-Path $logDir "exam-issue-core.out.log"
$errLog = Join-Path $logDir "exam-issue-core.err.log"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

$existing = Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($existing) {
    $process = Get-Process -Id $existing.OwningProcess -ErrorAction SilentlyContinue
    throw "8088 端口已被占用，当前进程: $($process.ProcessName)($($existing.OwningProcess))。请先手工停止旧进程后再启动。"
}

if ($Rebuild -or -not (Test-Path $targetJar)) {
    Write-Section "构建 issue-core"
    & "$repoRoot\mvnw.cmd" -pl exam-issue-core -am package -DskipTests
}

if (-not (Test-Path $targetJar)) {
    throw "未找到可启动的 jar: $targetJar"
}

Write-Section "启动 issue-core"
$env:EXAM_NACOS_ADDR = $NacosAddr
$env:EXAM_NACOS_USERNAME = $NacosUser
$env:EXAM_NACOS_PASSWORD = $NacosPassword
$env:EXAM_DB_HOST = $DbHost
$env:EXAM_DB_PORT = "$DbPort"
$env:EXAM_DB_NAME = $DbName
$env:EXAM_DB_USERNAME = $DbUser
$env:EXAM_DB_PASSWORD = $DbPassword
$env:SPRING_CLOUD_NACOS_DISCOVERY_FAIL_FAST = "false"

$process = Start-Process -FilePath "D:\Develop\Java\bin\java.exe" `
    -ArgumentList "-jar", $targetJar `
    -WorkingDirectory $moduleDir `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -PassThru

Start-Sleep -Seconds 15
$listening = Get-NetTCPConnection -LocalPort 8088 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $listening) {
    Write-Section "启动失败日志"
    if (Test-Path $outLog) {
        Get-Content $outLog -Tail 60
    }
    if (Test-Path $errLog) {
        Get-Content $errLog -Tail 60
    }
    throw "exam-issue-core 未成功监听 8088"
}

Write-Section "启动完成"
Write-Host "PID: $($process.Id)"
Write-Host "日志: $outLog"
Write-Host "端口: 8088"