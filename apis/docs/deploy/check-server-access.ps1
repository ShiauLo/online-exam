[CmdletBinding()]
param(
    [string]$RemoteHost = "root@47.105.121.232",
    [int]$SshPort = 22,
    [string]$RemoteRepoPath = "/opt/online-exam/apis/apis"
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Resolve-SshExe {
    $resolved = Get-Command "ssh.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved) {
        return $resolved.Source
    }

    $fallback = "C:\Windows\System32\OpenSSH\ssh.exe"
    if (Test-Path $fallback) {
        return $fallback
    }

    throw "未找到 ssh.exe，请先安装或加入 PATH。"
}

$sshExe = Resolve-SshExe

Write-Section "SSH 登录检查"
& $sshExe -o BatchMode=yes -o ConnectTimeout=8 -p $SshPort $RemoteHost "echo CONNECTED && whoami && uname -a"
if ($LASTEXITCODE -ne 0) {
    throw "SSH 免密登录失败。请先把本机公钥加入服务器 authorized_keys，或确认服务器允许当前登录方式。"
}

Write-Section "远端仓库检查"
$repoCheckOutput = & $sshExe -o BatchMode=yes -o ConnectTimeout=8 -p $SshPort $RemoteHost "test -d '$RemoteRepoPath' && echo REPO_OK || echo REPO_MISSING"
if ($LASTEXITCODE -ne 0) {
    throw "远端仓库目录检查失败：$RemoteRepoPath"
}
if (($repoCheckOutput | Out-String).Trim() -ne "REPO_OK") {
    throw "远端仓库目录不存在：$RemoteRepoPath。请先在服务器完成 git clone。"
}

Write-Section "运行时版本检查"
& $sshExe -o BatchMode=yes -o ConnectTimeout=8 -p $SshPort $RemoteHost "git --version && java -version && node -v && npm -v"
if ($LASTEXITCODE -ne 0) {
    throw "远端运行时版本检查失败。请确认 git/java/node/npm 已正确安装。"
}

Write-Section "GitHub SSH 检查"
$githubOutput = & $sshExe -o BatchMode=yes -o ConnectTimeout=8 -p $SshPort $RemoteHost "ssh -o BatchMode=yes -T git@github.com 2>&1 || true"
$githubText = ($githubOutput | Out-String).Trim()
Write-Host $githubText
if ($githubText -notmatch "successfully authenticated") {
    throw "服务器到 GitHub 的 SSH 仍未打通。请先把服务器公钥加入 GitHub Deploy Key 或 SSH Key。"
}

Write-Section "完成"
Write-Host "服务器基础接入检查已执行完毕。"
