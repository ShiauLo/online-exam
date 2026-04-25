[CmdletBinding()]
param(
    [string]$ApiBaseUrl = "http://127.0.0.1:8080",
    [string]$RealtimeBaseUrl = "http://127.0.0.1:8090",
    [string]$DbHost = "127.0.0.1",
    [int]$DbPort = 3306,
    [string]$DbName = "online_exam_db",
    [string]$DbUser = "root",
    [string]$DbPassword = "ChangeThisMySQLPassword_2026",
    [int]$SeedExamId = 8801,
    [int]$SeedStudentId = 4
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    try {
        $jsonBody = if ($null -eq $Body) { "{}" } else { $Body | ConvertTo-Json -Depth 10 }
        $response = Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType "application/json" -Body $jsonBody
        return [pscustomobject]@{
            ok = $true
            payload = $response
        }
    } catch {
        $message = $_.ErrorDetails.Message
        if (-not $message) {
            $message = $_.Exception.Message
        }
        return [pscustomobject]@{
            ok = $false
            payload = $message
        }
    }
}

function Test-ServiceUnavailable {
    param([object]$Response)

    $payloadText = if ($Response.payload -is [string]) {
        $Response.payload
    } else {
        $Response.payload | ConvertTo-Json -Depth 10 -Compress
    }

    return $payloadText -match '"status"\s*:\s*503' -or $payloadText -match 'Service Unavailable'
}

function New-TestTokens {
    $script = @"
const jwt = require('./exam-realtime/node_modules/jsonwebtoken');
const secret = 'mySuperSecretKeyThatIsAtLeast32BytesLongForHS512';
const build = (userId, roleId) => jwt.sign({ userId, roleId }, secret, { subject: String(userId), expiresIn: '30m' });
console.log(JSON.stringify({
  student: build(4, 4),
  teacher: build(3, 3),
  admin: build(2, 2),
  auditor: build(5, 5),
  ops: build(6, 6)
}));
"@
    $raw = node -e $script
    return $raw | ConvertFrom-Json
}

function Get-RequiredTables {
    param(
        [string]$DbServerHost,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [string]$Password
    )

    $script = @"
const mysql = require('./exam-realtime/node_modules/mysql2/promise');
(async () => {
  const conn = await mysql.createConnection({
    host: '$DbServerHost',
    port: $Port,
    user: '$User',
    password: '$Password',
    database: '$Database'
  });
  const required = [
    'exam_instance',
    'exam_instance_student',
    'exam_score_record',
    'exam_score_detail',
    'exam_issue_record',
    'exam_issue_process_log',
    'exam_realtime_abnormal_record'
  ];
  const [rows] = await conn.query(
    'SELECT table_name AS tableName FROM information_schema.tables WHERE table_schema = ? AND table_name IN (?,?,?,?,?,?,?) ORDER BY table_name',
    ['$Database', ...required]
  );
  console.log(JSON.stringify({
    required,
    existing: rows.map((item) => item.tableName)
  }));
  await conn.end();
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
"@
    $raw = node -e $script
    return $raw | ConvertFrom-Json
}

function Get-MinimumSeedState {
    param(
        [string]$DbServerHost,
        [int]$Port,
        [string]$Database,
        [string]$User,
        [string]$Password,
        [int]$ExamId,
        [int]$StudentId
    )

    $script = @"
const mysql = require('./exam-realtime/node_modules/mysql2/promise');
(async () => {
  const conn = await mysql.createConnection({
    host: '$DbServerHost',
    port: $Port,
    user: '$User',
    password: '$Password',
    database: '$Database'
  });
  const examId = $ExamId;
  const studentId = $StudentId;

  async function scalar(sql, params) {
    const [rows] = await conn.query(sql, params);
    const row = rows[0] || {};
    const firstKey = Object.keys(row)[0];
    return Number(row[firstKey] || 0);
  }

  const result = {
    examCount: await scalar('SELECT COUNT(*) AS total FROM exam_instance WHERE exam_id = ?', [examId]),
    classCount: await scalar('SELECT COUNT(*) AS total FROM exam_instance_class WHERE exam_id = ?', [examId]),
    studentRelationCount: await scalar('SELECT COUNT(*) AS total FROM exam_instance_student WHERE exam_id = ? AND student_id = ?', [examId, studentId]),
    scoreCount: await scalar('SELECT COUNT(*) AS total FROM exam_score_record WHERE exam_id = ? AND student_id = ?', [examId, studentId]),
    scoreDetailCount: await scalar('SELECT COUNT(*) AS total FROM exam_score_detail WHERE exam_id = ? AND student_id = ?', [examId, studentId])
  };

  console.log(JSON.stringify(result));
  await conn.end();
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
"@
    $raw = node -e $script
    return $raw | ConvertFrom-Json
}

Write-Section "端口检查"
$ports = 3306, 6379, 8848, 8080, 8081, 8082, 8086, 8090, 5173
foreach ($port in $ports) {
    $listening = netstat -ano | Select-String (":$port") | Select-String "LISTENING"
    if ($listening) {
        Write-Host "[OK] 端口 $port 已监听"
    } else {
        Write-Host "[WARN] 端口 $port 未监听"
    }
}

Write-Section "关键表检查"
$tableResult = Get-RequiredTables -DbServerHost $DbHost -Port $DbPort -Database $DbName -User $DbUser -Password $DbPassword
$missing = @($tableResult.required | Where-Object { $_ -notin $tableResult.existing })
Write-Host "已存在表: $($tableResult.existing -join ', ')"
if ($missing.Count -gt 0) {
    Write-Host "[WARN] 缺失表: $($missing -join ', ')"
} else {
    Write-Host "[OK] realtime/issue 联调关键表已齐"
}

Write-Section "最小种子检查"
$seedState = Get-MinimumSeedState -DbServerHost $DbHost -Port $DbPort -Database $DbName -User $DbUser -Password $DbPassword -ExamId $SeedExamId -StudentId $SeedStudentId
Write-Host "exam_instance: $($seedState.examCount)"
Write-Host "exam_instance_class: $($seedState.classCount)"
Write-Host "exam_instance_student: $($seedState.studentRelationCount)"
Write-Host "exam_score_record: $($seedState.scoreCount)"
Write-Host "exam_score_detail: $($seedState.scoreDetailCount)"

$seedReady = $seedState.examCount -gt 0 `
    -and $seedState.classCount -gt 0 `
    -and $seedState.studentRelationCount -gt 0 `
    -and $seedState.scoreCount -gt 0 `
    -and $seedState.scoreDetailCount -gt 0

if ($seedReady) {
    Write-Host "[OK] 最小联调种子已就绪（examId=$SeedExamId, studentId=$SeedStudentId）"
} else {
    Write-Host "[WARN] 最小联调种子未就绪（examId=$SeedExamId, studentId=$SeedStudentId）"
}

Write-Section "HTTP 冒烟"
$tokens = New-TestTokens

$issue401 = Invoke-JsonRequest -Method Post -Uri "$ApiBaseUrl/api/issue/notify"
Write-Host "[issue 401] $($issue401.payload)"

$teacherHeaders = @{
    Authorization = "Bearer $($tokens.teacher)"
    "X-Request-Id" = "smoke-realtime-teacher"
}
$teacherRealtime = Invoke-JsonRequest -Method Post -Uri "$ApiBaseUrl/api/exam/realtime/session" -Headers $teacherHeaders -Body @{ examId = $SeedExamId }
Write-Host "[realtime 403] $($teacherRealtime.payload)"

$studentHeaders = @{
    Authorization = "Bearer $($tokens.student)"
    "X-Request-Id" = "smoke-student-route"
}
$issueRoute = Invoke-JsonRequest -Method Post -Uri "$ApiBaseUrl/api/issue/notify" -Headers $studentHeaders
Write-Host "[issue route] $($issueRoute.payload)"

$systemRoute = Invoke-JsonRequest -Method Post -Uri "$ApiBaseUrl/api/system/permission/query" -Headers $studentHeaders -Body @{ accountId = $SeedStudentId }
Write-Host "[system route] $($systemRoute.payload)"

$realtimeRoute = Invoke-JsonRequest -Method Post -Uri "$ApiBaseUrl/api/exam/realtime/session" -Headers $studentHeaders -Body @{ examId = $SeedExamId }
Write-Host "[realtime route] $($realtimeRoute.payload)"

Write-Section "结论"
$realtimeUnavailable = Test-ServiceUnavailable -Response $teacherRealtime -or (Test-ServiceUnavailable -Response $realtimeRoute)
$issueUnavailable = Test-ServiceUnavailable -Response $issueRoute
$systemUnavailable = Test-ServiceUnavailable -Response $systemRoute

if ($missing.Count -gt 0) {
    Write-Host "联调服务已可启动，当前主要阻塞为数据库未同步到最新 schema。"
    Write-Host "建议先执行 docs/sql/mysql.sql 中缺失表对应的建表语句，再重新运行本脚本。"
} elseif ($realtimeUnavailable -or $issueUnavailable -or $systemUnavailable) {
    Write-Host "当前主要阻塞为网关路由到下游服务返回 503。"
    Write-Host "请优先检查 exam-account、exam-class、exam-core、exam-realtime 的启动状态、Nacos 注册状态和网关服务发现配置。"
} elseif (-not $seedReady) {
    Write-Host "当前主要阻塞为最小联调业务数据未初始化。"
    Write-Host "建议执行 scripts/sql/live-integration-seed.sql 后，再重新运行本脚本。"
} else {
    Write-Host "关键表已齐，可继续做真实业务数据联调。"
}
