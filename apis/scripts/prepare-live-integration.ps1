[CmdletBinding()]
param(
    [switch]$SkipReset,
    [switch]$EnsureRealtime,
    [switch]$RebuildRealtime,
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
    [int]$ExamId = 8801,
    [int]$StudentId = 4
)

$ErrorActionPreference = "Stop"
$scriptRoot = $PSScriptRoot
$repoRoot = Split-Path $scriptRoot -Parent
$resetScript = Join-Path $scriptRoot "reset-live-integration-state.ps1"
$smokeScript = Join-Path $scriptRoot "live-integration-smoke.ps1"
$realtimeScript = Join-Path $scriptRoot "start-realtime.ps1"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Test-ListeningPort {
    param([int]$Port)
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Show-PortState {
    param(
        [int]$Port,
        [string]$Name
    )
    if (Test-ListeningPort -Port $Port) {
        Write-Host "[OK] $Name($Port) 已监听"
    } else {
        Write-Host "[WARN] $Name($Port) 未监听"
    }
}

if (-not (Test-Path $resetScript)) {
    throw "未找到脚本: $resetScript"
}
if (-not (Test-Path $smokeScript)) {
    throw "未找到脚本: $smokeScript"
}
if (-not (Test-Path $realtimeScript)) {
    throw "未找到脚本: $realtimeScript"
}

Write-Section "关键端口预检查"
Show-PortState -Port 3306 -Name "MySQL"
Show-PortState -Port 6379 -Name "Redis"
Show-PortState -Port 8848 -Name "Nacos"
Show-PortState -Port 8080 -Name "exam-gateway"
Show-PortState -Port 8081 -Name "exam-account"
Show-PortState -Port 8082 -Name "exam-class"
Show-PortState -Port 8086 -Name "exam-core"
Show-PortState -Port 8090 -Name "exam-realtime"

if ($EnsureRealtime -and -not (Test-ListeningPort -Port 8090)) {
    Write-Section "补拉 realtime"
    $realtimeArgs = @(
        "-File", $realtimeScript,
        "-DbHost", $DbHost,
        "-DbPort", "$DbPort",
        "-DbName", $DbName,
        "-DbUser", $DbUser,
        "-DbPassword", $DbPassword,
        "-RedisHost", $RedisHost,
        "-RedisPort", "$RedisPort",
        "-RedisDatabase", "$RedisDatabase",
        "-RedisPassword", $RedisPassword,
        "-NacosAddr", $NacosAddr,
        "-NacosUser", $NacosUser,
        "-NacosPassword", $NacosPassword
    )
    if ($RebuildRealtime) {
        $realtimeArgs += "-Rebuild"
    }
    & pwsh @realtimeArgs
}

if (-not $SkipReset) {
    Write-Section "重置联调状态"
    & pwsh -File $resetScript `
        -DbHost $DbHost `
        -DbPort $DbPort `
        -DbName $DbName `
        -DbUser $DbUser `
        -DbPassword $DbPassword `
        -RedisHost $RedisHost `
        -RedisPort $RedisPort `
        -RedisDatabase $RedisDatabase `
        -RedisPassword $RedisPassword `
        -ExamId $ExamId `
        -StudentId $StudentId
}

Write-Section "运行联调冒烟"
& pwsh -File $smokeScript `
    -DbHost $DbHost `
    -DbPort $DbPort `
    -DbName $DbName `
    -DbUser $DbUser `
    -DbPassword $DbPassword `
    -SeedExamId $ExamId `
    -SeedStudentId $StudentId

Write-Section "准备完成"
Write-Host "推荐下一步："
Write-Host "1. 验证考试答题主链 /session -> enterExam -> save-progress -> report-abnormal -> submit"
Write-Host "2. 验证问题通知主链 create -> issueNotify -> handle -> processNotify -> track"
Write-Host "3. 若需真实登录，联调账号默认密码为 123456"
