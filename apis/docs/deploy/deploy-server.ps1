[CmdletBinding()]
param(
    [string[]]$Services,
    [switch]$All,
    [string]$RemoteHost = "root@47.105.121.232",
    [int]$SshPort = 22,
    [string]$RemoteRepoPath = "/opt/online-exam/apis/apis",
    [string]$RemoteRuntimeRoot = "/opt/online-exam",
    [string]$RemoteScriptPath = "",
    [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent

$allServices = @(
    "exam-gateway",
    "exam-account",
    "exam-class",
    "exam-core",
    "exam-realtime"
)

$allJavaServices = @(
    "exam-gateway",
    "exam-account",
    "exam-class",
    "exam-core"
)

$serviceDirectoryMap = [ordered]@{
    "exam-gateway" = "exam-gateway/"
    "exam-account" = "exam-account/"
    "exam-class" = "exam-class/"
    "exam-core" = "exam-core/"
    "exam-realtime" = "exam-realtime/"
}

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

function Invoke-Git {
    param(
        [string[]]$Arguments,
        [switch]$CaptureOutput
    )

    if ($CaptureOutput) {
        return (& $script:gitExe @Arguments 2>&1)
    }

    & $script:gitExe @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "git 命令执行失败：git $($Arguments -join ' ')"
    }
}

function Get-CurrentBranch {
    return (Invoke-Git -Arguments @("branch", "--show-current") -CaptureOutput).Trim()
}

function Assert-CleanWorktree {
    $status = Invoke-Git -Arguments @("status", "--porcelain") -CaptureOutput
    if (-not [string]::IsNullOrWhiteSpace(($status | Out-String))) {
        throw "当前工作区存在未提交改动。请先 commit 后再部署。"
    }
}

function Get-AheadBehind {
    param([string]$RemoteRef)

    $counts = (Invoke-Git -Arguments @("rev-list", "--left-right", "--count", "$RemoteRef...HEAD") -CaptureOutput).Trim()
    if (-not $counts) {
        throw "无法获取本地与 $RemoteRef 的提交差异。"
    }

    $parts = $counts -split "\s+"
    if ($parts.Length -lt 2) {
        throw "提交差异输出格式异常：$counts"
    }

    return [pscustomobject]@{
        Behind = [int]$parts[0]
        Ahead = [int]$parts[1]
    }
}

function Add-UniqueServices {
    param(
        [System.Collections.Generic.List[string]]$Target,
        [string[]]$Values
    )

    foreach ($value in $Values) {
        if (-not [string]::IsNullOrWhiteSpace($value) -and -not $Target.Contains($value)) {
            $Target.Add($value)
        }
    }
}

function Resolve-ServicesFromChanges {
    param([string[]]$ChangedFiles)

    $result = New-Object System.Collections.Generic.List[string]
    foreach ($file in $ChangedFiles) {
        if ([string]::IsNullOrWhiteSpace($file)) {
            continue
        }

        if ($file -eq "pom.xml" -or $file.StartsWith("exam-common/") -or $file.StartsWith("config/")) {
            Add-UniqueServices -Target $result -Values $allJavaServices
            continue
        }

        # 兼容物理合并前的历史目录，便于识别“删除旧模块”这类变更仍应落到新宿主。
        if ($file.StartsWith("exam-system/")) {
            Add-UniqueServices -Target $result -Values @("exam-account")
            continue
        }

        if ($file.StartsWith("exam-question/") -or $file.StartsWith("exam-paper/") -or $file.StartsWith("exam-resource/")) {
            Add-UniqueServices -Target $result -Values @("exam-class")
            continue
        }

        if ($file.StartsWith("exam-score/") -or $file.StartsWith("exam-issue-core/")) {
            Add-UniqueServices -Target $result -Values @("exam-core")
            continue
        }

        if ($file.StartsWith("exam-issue-notify/")) {
            Add-UniqueServices -Target $result -Values @("exam-realtime")
            continue
        }

        if ($file.StartsWith("docs/") -or $file.StartsWith("scripts/") -or $file.StartsWith("web/")) {
            continue
        }

        foreach ($serviceName in $serviceDirectoryMap.Keys) {
            if ($file.StartsWith($serviceDirectoryMap[$serviceName])) {
                Add-UniqueServices -Target $result -Values @($serviceName)
                break
            }
        }
    }

    return @($result.ToArray())
}

function Resolve-TargetServices {
    if ($All -and $Services) {
        throw "-All 与 -Services 不能同时使用。"
    }

    if ($All) {
        return $allServices
    }

    if ($Services) {
        $selected = New-Object System.Collections.Generic.List[string]
        foreach ($service in $Services) {
            foreach ($item in ($service -split ",")) {
                $trimmed = $item.Trim()
                if ([string]::IsNullOrWhiteSpace($trimmed)) {
                    continue
                }
                if ($allServices -notcontains $trimmed) {
                    throw "不支持的服务名：$trimmed。可选值：$($allServices -join ', ')"
                }
                Add-UniqueServices -Target $selected -Values @($trimmed)
            }
        }
        return @($selected.ToArray())
    }

    $changedFiles = Invoke-Git -Arguments @("diff", "--name-only", "origin/$Branch...HEAD") -CaptureOutput
    $resolved = Resolve-ServicesFromChanges -ChangedFiles $changedFiles
    if ($resolved.Count -eq 0) {
        throw "未检测到需要部署的后端服务。若需强制部署，请传 -All 或 -Services。"
    }

    return $resolved
}

function Invoke-RemoteCommand {
    param([string]$Command)

    & $script:sshExe "-p" "$SshPort" $RemoteHost $Command
    if ($LASTEXITCODE -ne 0) {
        throw "远程命令执行失败：$Command"
    }
}

$gitExe = Resolve-Executable -CommandName "git.exe" -FallbackPaths @()
$sshExe = Resolve-Executable -CommandName "ssh.exe" -FallbackPaths @(
    "C:\Windows\System32\OpenSSH\ssh.exe"
)

if ([string]::IsNullOrWhiteSpace($RemoteScriptPath)) {
    $RemoteScriptPath = "$RemoteRepoPath/scripts/deploy/server-redeploy.sh"
}

Write-Section "本地检查"
Assert-CleanWorktree

$currentBranch = Get-CurrentBranch
if ($currentBranch -ne $Branch) {
    throw "当前分支是 $currentBranch，本脚本仅允许从 $Branch 分支部署。"
}

Write-Host "当前分支: $currentBranch"
Write-Host "远程主机: $RemoteHost"
Write-Host "服务器仓库: $RemoteRepoPath"

Write-Section "同步 Git 状态"
Invoke-Git -Arguments @("fetch", "origin", $Branch)
$aheadBehind = Get-AheadBehind -RemoteRef "origin/$Branch"

if ($aheadBehind.Behind -gt 0 -and $aheadBehind.Ahead -gt 0) {
    throw "本地分支与 origin/$Branch 已分叉。请先手工 rebase 或 merge 后再部署。"
}

if ($aheadBehind.Behind -gt 0) {
    throw "本地分支落后于 origin/$Branch。请先拉取并处理冲突后再部署。"
}

if ($aheadBehind.Ahead -gt 0) {
    Write-Host "检测到本地领先远端 $($aheadBehind.Ahead) 个提交，开始推送..."
    Invoke-Git -Arguments @("push", "origin", "$Branch")
} else {
    Write-Host "本地与 origin/$Branch 已同步，无需 push。"
}

$targetServices = Resolve-TargetServices
Write-Section "部署范围"
Write-Host ($targetServices -join ", ")

Write-Section "远端检查"
$remoteCheckCommand = "test -f '$RemoteScriptPath'"
Invoke-RemoteCommand -Command $remoteCheckCommand

Write-Section "开始远程部署"
$serviceCsv = $targetServices -join ","
$remoteDeployCommand = if ($All) {
    "bash '$RemoteScriptPath' --repo '$RemoteRepoPath' --runtime-root '$RemoteRuntimeRoot' --all"
} else {
    "bash '$RemoteScriptPath' --repo '$RemoteRepoPath' --runtime-root '$RemoteRuntimeRoot' --services '$serviceCsv'"
}
Invoke-RemoteCommand -Command $remoteDeployCommand

Write-Section "部署完成"
Write-Host "已完成远程更新与定向重启。"
