[CmdletBinding()]
param(
    [switch]$EnsureRealtime,
    [switch]$RebuildRealtime,
    [int]$StepTimeoutSeconds = 180,
    [string]$ApiBaseUrl = "http://127.0.0.1:8080",
    [string]$RealtimeSocketBaseUrl = "http://127.0.0.1:8090",
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
    [int]$ClassId = 501,
    [int]$StudentId = 4,
    [int]$TeacherId = 3
)

$ErrorActionPreference = "Stop"
$prepareScript = Join-Path $PSScriptRoot "prepare-live-integration.ps1"
$examScript = Join-Path $PSScriptRoot "verify-live-exam-flow.ps1"
$issueScript = Join-Path $PSScriptRoot "verify-live-issue-flow.ps1"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    $start = Get-Date
    try {
        & $Action | Out-Host
        $duration = [int]((Get-Date) - $start).TotalSeconds
        return [pscustomobject]@{
            name = $Name
            ok = $true
            durationSeconds = $duration
        }
    } catch {
        $duration = [int]((Get-Date) - $start).TotalSeconds
        return [pscustomobject]@{
            name = $Name
            ok = $false
            durationSeconds = $duration
            error = $_.Exception.Message
        }
    }
}

function Invoke-ExternalPwshScript {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$ArgumentList
    )
    $safeName = ($Name -replace "[^a-zA-Z0-9\-]", "_")
    $stdoutPath = Join-Path $env:TEMP ("$safeName-stdout.log")
    $stderrPath = Join-Path $env:TEMP ("$safeName-stderr.log")
    foreach ($path in @($stdoutPath, $stderrPath)) {
        if (Test-Path $path) {
            Remove-Item $path -Force
        }
    }
    $process = $null
    try {
        $pwshArgs = @("-NoLogo", "-NoProfile", "-File", $FilePath) + $ArgumentList
        $process = Start-Process -FilePath "pwsh" `
            -ArgumentList $pwshArgs `
            -PassThru `
            -WindowStyle Hidden `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath
        if (-not $process.WaitForExit($StepTimeoutSeconds * 1000)) {
            Stop-Process -Id $process.Id -Force
            throw "$Name 超时，已超过 $StepTimeoutSeconds 秒。"
        }
        if (Test-Path $stdoutPath) {
            Get-Content $stdoutPath -Encoding UTF8 | Out-Host
        }
        if (Test-Path $stderrPath) {
            $stderrContent = Get-Content $stderrPath -Encoding UTF8
            if ($stderrContent.Count -gt 0) {
                $stderrContent | Out-Host
            }
        }
        if ($process.ExitCode -ne 0) {
            throw "$Name 失败，退出码: $($process.ExitCode)"
        }
    } finally {
        foreach ($path in @($stdoutPath, $stderrPath)) {
            if (Test-Path $path) {
                Remove-Item $path -Force
            }
        }
    }
}

Write-Section "真实联调总验收"
$results = @()

$results += Invoke-Step -Name "环境准备" -Action {
    $args = @(
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
        "-NacosPassword", $NacosPassword,
        "-ExamId", "$ExamId",
        "-StudentId", "$StudentId"
    )
    if ($EnsureRealtime) {
        $args += "-EnsureRealtime"
    }
    if ($RebuildRealtime) {
        $args += "-RebuildRealtime"
    }
    Invoke-ExternalPwshScript -Name "prepare-live-integration" -FilePath $prepareScript -ArgumentList $args
}

$results += Invoke-Step -Name "考试答题主链" -Action {
    Invoke-ExternalPwshScript -Name "verify-live-exam-flow" -FilePath $examScript -ArgumentList @(
        "-ApiBaseUrl", $ApiBaseUrl,
        "-RealtimeSocketBaseUrl", $RealtimeSocketBaseUrl,
        "-DbHost", $DbHost,
        "-DbPort", "$DbPort",
        "-DbName", $DbName,
        "-DbUser", $DbUser,
        "-DbPassword", $DbPassword,
        "-RedisHost", $RedisHost,
        "-RedisPort", "$RedisPort",
        "-RedisDatabase", "$RedisDatabase",
        "-RedisPassword", $RedisPassword,
        "-ExamId", "$ExamId",
        "-StudentId", "$StudentId"
    )
}

$results += Invoke-Step -Name "问题通知主链" -Action {
    Invoke-ExternalPwshScript -Name "verify-live-issue-flow" -FilePath $issueScript -ArgumentList @(
        "-ApiBaseUrl", $ApiBaseUrl,
        "-ExamId", "$ExamId",
        "-ClassId", "$ClassId",
        "-StudentId", "$StudentId",
        "-TeacherId", "$TeacherId"
    )
}

Write-Section "验收摘要"
foreach ($item in $results) {
    if ($item.ok) {
        Write-Host "[OK] $($item.name) 通过 ($($item.durationSeconds)s)"
    } else {
        Write-Host "[FAIL] $($item.name) 失败 ($($item.durationSeconds)s)"
        Write-Host "原因: $($item.error)"
    }
}

$failed = @($results | Where-Object { -not $_.ok })
if ($failed.Count -gt 0) {
    throw "真实联调整体验收失败，请先处理上述失败步骤。"
}

Write-Section "最终结果"
Write-Host "真实联调整体验收通过。"
