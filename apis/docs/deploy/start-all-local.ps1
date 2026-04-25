[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$StartTunnel,
    [switch]$UseNacos,
    [switch]$SkipJavaBuild,
    [switch]$SkipNodeBuild,
    [switch]$SkipWeb,
    [string]$MavenRepoLocal = "",
    [int]$TunnelWaitSeconds = 90,
    [int]$JavaWaitSeconds = 25,
    [int]$NodeWaitSeconds = 15,
    [int]$WebWaitSeconds = 15
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$logDir = Join-Path $repoRoot ".runtime-logs"
$pidFile = Join-Path $logDir "local-dev-stack.processes.json"
$mvnwCmd = Join-Path $repoRoot "mvnw.cmd"
$tunnelScript = Join-Path $PSScriptRoot "start-tunnel.bat"

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
}

if ([string]::IsNullOrWhiteSpace($MavenRepoLocal)) {
    $MavenRepoLocal = Join-Path $repoRoot ".m2"
}

$javaServices = @(
    @{ Name = "exam-account"; Port = 8081; Module = "exam-account"; Jar = "target/exam-account-0.0.1-SNAPSHOT.jar"; ConfigName = "exam-account-host" },
    @{ Name = "exam-class"; Port = 8082; Module = "exam-class"; Jar = "target/exam-class-0.0.1-SNAPSHOT.jar"; ConfigName = "exam-class-host" },
    @{ Name = "exam-core"; Port = 8086; Module = "exam-core"; Jar = "target/exam-core-0.0.1-SNAPSHOT.jar"; ConfigName = "exam-core-host" },
    @{ Name = "exam-gateway"; Port = 8080; Module = "exam-gateway"; Jar = "target/exam-gateway-0.0.1-SNAPSHOT.jar"; ConfigName = "" }
)

$nodeServices = @(
    @{ Name = "exam-realtime"; Port = 8090; Module = "exam-realtime"; Entry = "dist/src/server.js" }
)

$frontendService = @{ Name = "web"; Port = 5173; Module = "web" }
$startedProcesses = New-Object System.Collections.Generic.List[object]
$singleHostMode = -not $UseNacos.IsPresent
$script:startupEnvKeys = @(
    "EXAM_NACOS_ENABLED",
    "EXAM_ACCOUNT_ROUTE_URI",
    "EXAM_CLASS_ROUTE_URI",
    "EXAM_CORE_ROUTE_URI",
    "EXAM_REALTIME_ROUTE_URI",
    "EXAM_REALTIME_EXAM_CORE_BASE_URL"
)
$script:originalStartupEnv = @{}

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Resolve-Executable {
    param(
        [string]$CommandName,
        [string[]]$FallbackPaths
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

function Get-PortProcessInfo {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $connection) {
        return $null
    }

    $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
    return [pscustomobject]@{
        Port = $Port
        Pid = $connection.OwningProcess
        Name = if ($process) { $process.ProcessName } else { "unknown" }
    }
}

function Assert-PortFree {
    param(
        [int]$Port,
        [string]$ServiceName
    )

    $info = Get-PortProcessInfo -Port $Port
    if ($info) {
        throw "$ServiceName 需要使用端口 $Port，但当前已被 $($info.Name)($($info.Pid)) 占用。请先停止旧进程后再重试。"
    }
}

function Test-PortListening {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Wait-PortListening {
    param(
        [int]$Port,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1) {
            return $true
        }
        Start-Sleep -Seconds 1
    }

    return $false
}

function Wait-PortsListening {
    param(
        [int[]]$Ports,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $allReady = $true
        foreach ($port in $Ports) {
            if (-not (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)) {
                $allReady = $false
                break
            }
        }
        if ($allReady) {
            return $true
        }
        Start-Sleep -Seconds 1
    }

    return $false
}

function Save-ProcessFile {
    $startedProcesses | ConvertTo-Json -Depth 5 | Set-Content -Path $pidFile -Encoding UTF8
}

function Push-StartupEnvironment {
    param([hashtable]$Values)

    foreach ($key in $script:startupEnvKeys) {
        if (-not $script:originalStartupEnv.ContainsKey($key)) {
            $script:originalStartupEnv[$key] = [Environment]::GetEnvironmentVariable($key, "Process")
        }
    }

    foreach ($key in $script:startupEnvKeys) {
        if ($Values.ContainsKey($key)) {
            [Environment]::SetEnvironmentVariable($key, [string]$Values[$key], "Process")
        } else {
            [Environment]::SetEnvironmentVariable($key, $null, "Process")
        }
    }
}

function Pop-StartupEnvironment {
    foreach ($key in $script:startupEnvKeys) {
        if ($script:originalStartupEnv.ContainsKey($key)) {
            [Environment]::SetEnvironmentVariable($key, $script:originalStartupEnv[$key], "Process")
        } else {
            [Environment]::SetEnvironmentVariable($key, $null, "Process")
        }
    }
}

function Register-StartedProcess {
    param(
        [string]$Name,
        [int]$ProcessId,
        [int]$Port = 0,
        [string]$Module,
        [string]$OutLog,
        [string]$ErrLog
    )

    $startedProcesses.Add([pscustomobject]@{
        name = $Name
        pid = $ProcessId
        port = $Port
        module = $Module
        outLog = $OutLog
        errLog = $ErrLog
        startedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    }) | Out-Null
    Save-ProcessFile
}

function Get-ServiceEnvironment {
    param([string]$ServiceName)

    if (-not $singleHostMode) {
        return @{}
    }

    $values = @{
        EXAM_NACOS_ENABLED = "false"
    }

    if ($ServiceName -eq "exam-gateway") {
        $values["EXAM_ACCOUNT_ROUTE_URI"] = "http://127.0.0.1:8081"
        $values["EXAM_CLASS_ROUTE_URI"] = "http://127.0.0.1:8082"
        $values["EXAM_CORE_ROUTE_URI"] = "http://127.0.0.1:8086"
        $values["EXAM_REALTIME_ROUTE_URI"] = "http://127.0.0.1:8090"
    }

    if ($ServiceName -eq "exam-realtime") {
        $values["EXAM_REALTIME_EXAM_CORE_BASE_URL"] = "http://127.0.0.1:8086"
    }

    return $values
}

function Start-SshTunnelIfNeeded {
    if (-not $StartTunnel) {
        return
    }

    if ((Test-PortListening -Port 3306) -and (Test-PortListening -Port 6379) -and (Test-PortListening -Port 8848)) {
        Write-Section "SSH 隧道检查"
        Write-Host "3306/6379/8848 已可访问，跳过新建 SSH 隧道。"
        return
    }

    if (-not (Test-Path $tunnelScript)) {
        throw "未找到 SSH 隧道脚本：$tunnelScript"
    }

    Write-Section "启动 SSH 隧道"
    Write-Host "将打开一个新的命令行窗口执行 start-tunnel.bat。"
    Write-Host "如果提示输入密码，请在新窗口中完成。"

    $tunnelProcess = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c", "`"$tunnelScript`"" `
        -WorkingDirectory $PSScriptRoot `
        -WindowStyle Normal `
        -PassThru

    if (Wait-PortsListening -Ports @(3306, 6379, 8848) -TimeoutSeconds $TunnelWaitSeconds) {
        Register-StartedProcess `
            -Name "ssh-tunnel" `
            -ProcessId $tunnelProcess.Id `
            -Module "docs/deploy" `
            -OutLog "" `
            -ErrLog ""
        Write-Host "SSH 隧道已就绪，关键端口 3306/6379/8848 可用。"
        return
    }

    if ($tunnelProcess -and -not $tunnelProcess.HasExited) {
        try {
            Stop-Process -Id $tunnelProcess.Id -Force
        } catch {
            Write-Warning "SSH 隧道等待超时后停止进程失败: $($_.Exception.Message)"
        }
    }

    throw "SSH 隧道在 ${TunnelWaitSeconds}s 内未就绪。请检查新打开的隧道窗口是否要求输入密码，或端口是否被其他程序占用。"
}

function Stop-StartedProcesses {
    if ($startedProcesses.Count -eq 0) {
        return
    }

    Write-Section "回滚已启动进程"
    foreach ($item in ($startedProcesses | Sort-Object -Property port -Descending)) {
        try {
            $process = Get-Process -Id $item.pid -ErrorAction SilentlyContinue
            if ($process) {
                Stop-Process -Id $item.pid -Force
                Write-Host "已停止 $($item.name) ($($item.pid))"
            }
        } catch {
            Write-Warning "停止 $($item.name) ($($item.pid)) 失败: $($_.Exception.Message)"
        }
    }
}

function Start-ManagedProcess {
    param(
        [string]$Name,
        [string]$Module,
        [int]$Port,
        [string]$FilePath,
        [string[]]$Arguments,
        [int]$WaitSeconds
    )

    $existing = Get-PortProcessInfo -Port $Port
    if ($existing) {
        Write-Host "$Name 端口 $Port 已由 $($existing.Name)($($existing.Pid)) 占用，跳过启动并复用现有进程。"
        return
    }

    $moduleDir = Join-Path $repoRoot $Module
    $outLog = Join-Path $logDir "$Name.out.log"
    $errLog = Join-Path $logDir "$Name.err.log"

    if (Test-Path $outLog) {
        Remove-Item $outLog -Force
    }
    if (Test-Path $errLog) {
        Remove-Item $errLog -Force
    }

    $serviceEnv = Get-ServiceEnvironment -ServiceName $Name
    Push-StartupEnvironment -Values $serviceEnv
    try {
        $process = Start-Process -FilePath $FilePath `
            -ArgumentList $Arguments `
            -WorkingDirectory $moduleDir `
            -RedirectStandardOutput $outLog `
            -RedirectStandardError $errLog `
            -PassThru
    } finally {
        Pop-StartupEnvironment
    }

    if (-not (Wait-PortListening -Port $Port -TimeoutSeconds $WaitSeconds)) {
        $accessDenied = $false
        if (Test-Path $outLog) {
            Write-Host ""
            Write-Host "[$Name] 标准输出日志尾部："
            Get-Content $outLog -Tail 80
            $accessDenied = [bool](Select-String -Path $outLog -Pattern "Access denied for user" -SimpleMatch -ErrorAction SilentlyContinue)
        }
        if (Test-Path $errLog) {
            Write-Host ""
            Write-Host "[$Name] 错误日志尾部："
            Get-Content $errLog -Tail 80
            if (-not $accessDenied) {
                $accessDenied = [bool](Select-String -Path $errLog -Pattern "Access denied for user" -SimpleMatch -ErrorAction SilentlyContinue)
            }
        }
        if ($accessDenied) {
            throw "$Name 未在 ${WaitSeconds}s 内监听端口 $Port。已识别为 MySQL 账号授权失败，请先按 docs/deploy/README.md 中的 本地 MySQL root 授权快速修复 处理。"
        }
        throw "$Name 未在 ${WaitSeconds}s 内监听端口 $Port。"
    }

    Register-StartedProcess -Name $Name -ProcessId $process.Id -Port $Port -Module $Module -OutLog $outLog -ErrLog $errLog
    Write-Host "$Name 已启动，PID=$($process.Id)，端口=$Port"
}

try {
    $javaExe = Resolve-Executable -CommandName "java.exe" -FallbackPaths @(
        "D:\Develop\Java\bin\java.exe",
        "C:\Program Files\Java\jdk-21\bin\java.exe"
    )
    $nodeExe = Resolve-Executable -CommandName "node.exe" -FallbackPaths @(
        "D:\Develop\node.js\node.exe",
        "C:\Program Files\nodejs\node.exe"
    )
    $npmCmd = Resolve-Executable -CommandName "npm.cmd" -FallbackPaths @(
        "D:\Develop\node.js\npm.cmd",
        "C:\Program Files\nodejs\npm.cmd"
    )

    Write-Section "启动前检查"
    Start-SshTunnelIfNeeded
    Write-Host "启动脚本将复用已在监听端口的现有服务，仅补拉尚未启动的服务。"
    if ($singleHostMode) {
        Write-Host "当前模式: 单机直连模式（不依赖 Nacos，网关直接转发到 127.0.0.1）"
    } else {
        Write-Host "当前模式: 注册中心模式（依赖 Nacos 服务发现）"
    }

    $needJavaBuild = $Rebuild.IsPresent
    if (-not $needJavaBuild) {
        foreach ($service in $javaServices) {
            $jarPath = Join-Path (Join-Path $repoRoot $service.Module) $service.Jar
            if (-not (Test-Path $jarPath)) {
                $needJavaBuild = $true
                break
            }
        }
    }

    if ($needJavaBuild -and -not $SkipJavaBuild) {
        Write-Section "构建全部 Java 服务"
        & $mvnwCmd package -DskipTests "-Dmaven.repo.local=$MavenRepoLocal"
        if ($LASTEXITCODE -ne 0) {
            throw "Maven 构建失败，请先修复编译错误。"
        }
    }

    foreach ($service in $nodeServices) {
        $moduleDir = Join-Path $repoRoot $service.Module
        $entryFile = Join-Path $moduleDir $service.Entry
        $nodeModules = Join-Path $moduleDir "node_modules"
        if (-not (Test-Path $nodeModules)) {
            Write-Section "安装 $($service.Name) 依赖"
            Push-Location $moduleDir
            try {
                & $npmCmd install
                if ($LASTEXITCODE -ne 0) {
                    throw "$($service.Name) npm install 失败。"
                }
            } finally {
                Pop-Location
            }
        }
        if (($Rebuild -or -not (Test-Path $entryFile)) -and -not $SkipNodeBuild) {
            Write-Section "构建 $($service.Name)"
            Push-Location $moduleDir
            try {
                & $npmCmd run build
                if ($LASTEXITCODE -ne 0) {
                    throw "$($service.Name) npm run build 失败。"
                }
            } finally {
                Pop-Location
            }
        }
    }

    if (-not $SkipWeb) {
        $webDir = Join-Path $repoRoot $frontendService.Module
        $webNodeModules = Join-Path $webDir "node_modules"
        if (-not (Test-Path $webNodeModules)) {
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
    }

    Write-Section "启动 Java 服务"
    foreach ($service in $javaServices) {
        $jarPath = Join-Path (Join-Path $repoRoot $service.Module) $service.Jar
        if (-not (Test-Path $jarPath)) {
            throw "$($service.Name) 缺少可启动 jar：$jarPath"
        }

        Start-ManagedProcess `
            -Name $service.Name `
            -Module $service.Module `
            -Port $service.Port `
            -FilePath $javaExe `
            -Arguments @("-jar", $jarPath, "--spring.profiles.active=dev,local") + $(if ([string]::IsNullOrWhiteSpace($service.ConfigName)) { @() } else { @("--spring.config.name=$($service.ConfigName)") }) `
            -WaitSeconds $JavaWaitSeconds
    }

    Write-Section "启动 Node 服务"
    foreach ($service in $nodeServices) {
        $entryFile = Join-Path (Join-Path $repoRoot $service.Module) $service.Entry
        if (-not (Test-Path $entryFile)) {
            throw "$($service.Name) 缺少可启动入口：$entryFile"
        }

        Start-ManagedProcess `
            -Name $service.Name `
            -Module $service.Module `
            -Port $service.Port `
            -FilePath $nodeExe `
            -Arguments @($entryFile) `
            -WaitSeconds $NodeWaitSeconds
    }

    if (-not $SkipWeb) {
        Write-Section "启动前端"
        $viteEntry = Join-Path (Join-Path $repoRoot $frontendService.Module) "node_modules/vite/bin/vite.js"
        if (-not (Test-Path $viteEntry)) {
            throw "未找到 Vite 启动入口：$viteEntry"
        }

        Start-ManagedProcess `
            -Name $frontendService.Name `
            -Module $frontendService.Module `
            -Port $frontendService.Port `
            -FilePath $nodeExe `
            -Arguments @($viteEntry, "--host", "127.0.0.1", "--port", "5173", "--strictPort") `
            -WaitSeconds $WebWaitSeconds
    }

    Write-Section "启动完成"
    Write-Host "进程记录: $pidFile"
    Write-Host "日志目录: $logDir"
    Write-Host ""
    Write-Host "访问入口："
    Write-Host "- 网关: http://127.0.0.1:8080"
    Write-Host "- 前端: http://127.0.0.1:5173"
    Write-Host "- 实时答题 WS: http://127.0.0.1:8090  path=/socket.io"
    Write-Host "- 问题通知 WS: http://127.0.0.1:8090  path=/issue-socket.io"
} catch {
    $message = $_.Exception.Message
    Stop-StartedProcesses
    throw $message
}
