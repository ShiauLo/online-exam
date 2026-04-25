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
    [int]$ExamId = 8801,
    [int]$StudentId = 4
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

$seedScriptPath = Join-Path $PSScriptRoot "sql/live-integration-seed.sql"
if (-not (Test-Path $seedScriptPath)) {
    throw "未找到联调种子脚本: $seedScriptPath"
}

Write-Section "重置数据库种子"
$seedScript = @"
const fs = require('node:fs');
const mysql = require('./exam-realtime/node_modules/mysql2/promise');

(async () => {
  const sql = fs.readFileSync('$($seedScriptPath -replace '\\', '\\\\')', 'utf8');
  const conn = await mysql.createConnection({
    host: '$DbHost',
    port: $DbPort,
    user: '$DbUser',
    password: '$DbPassword',
    database: '$DbName',
    multipleStatements: true
  });
  await conn.query(sql);
  const [rows] = await conn.query(
    'SELECT score_id, exam_id, student_id, status, submitted_at, request_id FROM exam_score_record WHERE exam_id = ? AND student_id = ?',
    [$ExamId, $StudentId]
  );
  console.log(JSON.stringify(rows, null, 2));
  await conn.end();
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
"@
node -e $seedScript

Write-Section "清理 Redis 联调状态"
$redisScript = @"
const redis = require('./exam-realtime/node_modules/redis');

(async () => {
  const client = redis.createClient({
    socket: {
      host: '$RedisHost',
      port: $RedisPort
    },
    database: $RedisDatabase,
    password: '$RedisPassword'
  });
  await client.connect();
  const keys = [
    'exam:realtime:draft:${ExamId}:${StudentId}',
    'exam:realtime:socket:${ExamId}:${StudentId}',
    'exam:realtime:submit-lock:${ExamId}:${StudentId}'
  ];
  const removed = await client.del(keys);
  console.log(JSON.stringify({ keys, removed }, null, 2));
  await client.quit();
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
"@
node -e $redisScript

Write-Section "建议下一步"
Write-Host "1. 运行 pwsh -File .\\scripts\\live-integration-smoke.ps1"
Write-Host "2. 再执行考试答题主链联调"
Write-Host "3. 联调账号默认密码已统一重置为 123456"