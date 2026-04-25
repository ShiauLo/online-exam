[CmdletBinding()]
param(
    [switch]$PrepareState,
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
    [int]$ExamId = 8801,
    [int]$StudentId = 4
)

$ErrorActionPreference = "Stop"
$prepareScript = Join-Path $PSScriptRoot "prepare-live-integration.ps1"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

if ($PrepareState) {
    Write-Section "预处理联调环境"
    & pwsh -File $prepareScript `
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

Write-Section "执行考试答题主链"
$script = @"
const jwt = require('./exam-realtime/node_modules/jsonwebtoken');
const { io } = require('./exam-realtime/node_modules/socket.io-client');
const mysql = require('./exam-realtime/node_modules/mysql2/promise');

const secret = 'mySuperSecretKeyThatIsAtLeast32BytesLongForHS512';
const token = jwt.sign({ userId: $StudentId, roleId: 4 }, secret, { subject: '$StudentId', expiresIn: '30m' });

function headers(requestId) {
  return {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token,
    'X-Request-Id': requestId
  };
}

async function call(method, url, body, requestId) {
  const response = await fetch(url, {
    method,
    headers: headers(requestId),
    body: JSON.stringify(body ?? {})
  });
  const text = await response.text();
  return { status: response.status, json: JSON.parse(text) };
}

async function waitForSocketFlow(requestId) {
  return await new Promise((resolve, reject) => {
    const client = io('$RealtimeSocketBaseUrl', {
      auth: { token, requestId },
      timeout: 10000,
      transports: ['websocket']
    });
    const result = { connected: null, countdowns: [], processNotify: [] };
    let finished = false;
    const timer = setTimeout(() => done(new Error('socket 等待超时'), true), 12000);

    function done(value, isError = false) {
      if (finished) return;
      finished = true;
      clearTimeout(timer);
      client.disconnect();
      if (isError) reject(value);
      else resolve(value);
    }

    client.on('connect', () => client.emit('enterExam', { examId: $ExamId }));
    client.on('connected', (payload) => {
      result.connected = payload;
      client.emit('reportScreen', { examId: $ExamId, screenOutCount: 1 });
    });
    client.on('countdown', (payload) => {
      result.countdowns.push(payload);
      if (result.connected && result.countdowns.length >= 2 && result.processNotify.length >= 1) {
        done(result);
      }
    });
    client.on('processNotify', (payload) => {
      result.processNotify.push(payload);
      if (result.connected && result.countdowns.length >= 2) {
        done(result);
      }
    });
    client.on('connect_error', (error) => done(error instanceof Error ? error : new Error(String(error)), true));
  });
}

(async () => {
  const requestId = 'verify-live-exam-' + Date.now();
  const answersBeforeSubmit = {
    '7001': 'A',
    '7002': '仅验证保存回读'
  };
  const answersForSubmit = {
    '7001': 'A',
    '7002': 'JVM 包含堆、栈、方法区'
  };

  const sessionBefore = await call('POST', '$ApiBaseUrl/api/exam/realtime/session', { examId: $ExamId }, requestId);
  const socketResult = await waitForSocketFlow(requestId);
  const saveResult = await call('POST', '$ApiBaseUrl/api/exam/realtime/save-progress', {
    examId: $ExamId,
    answers: answersBeforeSubmit
  }, requestId);
  const sessionAfterSave = await call('POST', '$ApiBaseUrl/api/exam/realtime/session', { examId: $ExamId }, requestId);
  const abnormalResult = await call('POST', '$ApiBaseUrl/api/exam/realtime/report-abnormal', {
    examId: $ExamId,
    type: 'screen-out',
    desc: '验收脚本触发一次切屏上报',
    imgUrls: []
  }, requestId);
  const submitResult = await call('PUT', '$ApiBaseUrl/api/exam/realtime/submit', {
    examId: $ExamId,
    answers: answersForSubmit
  }, requestId);
  const sessionAfterSubmit = await call('POST', '$ApiBaseUrl/api/exam/realtime/session', { examId: $ExamId }, requestId);

  const conn = await mysql.createConnection({
    host: '$DbHost',
    port: $DbPort,
    user: '$DbUser',
    password: '$DbPassword',
    database: '$DbName'
  });
  const [scoreRows] = await conn.query(
    'SELECT status, submitted_at, total_score, objective_score, subjective_score FROM exam_score_record WHERE exam_id = ? AND student_id = ?',
    [$ExamId, $StudentId]
  );
  const [detailRows] = await conn.query(
    'SELECT question_id, student_answer, score, is_correct FROM exam_score_detail WHERE exam_id = ? AND student_id = ? ORDER BY question_id',
    [$ExamId, $StudentId]
  );
  const [abnormalRows] = await conn.query(
    'SELECT abnormal_id, type, description, screen_out_count, request_id FROM exam_realtime_abnormal_record WHERE exam_id = ? AND reporter_id = ? ORDER BY create_time DESC LIMIT 1',
    [$ExamId, $StudentId]
  );
  await conn.end();

  const ok =
    sessionBefore.status === 200 &&
    saveResult.status === 200 &&
    sessionAfterSave.status === 200 &&
    abnormalResult.status === 200 &&
    submitResult.status === 200 &&
    sessionAfterSubmit.status === 409 &&
    socketResult.connected &&
    socketResult.countdowns.length >= 2 &&
    socketResult.processNotify.length >= 1 &&
    sessionAfterSave.json.data.answers['7001'] === 'A' &&
    submitResult.json.data.status === 'SCORING' &&
    Array.isArray(scoreRows) &&
    scoreRows[0] &&
    scoreRows[0].status === 'SCORING';

  console.log(JSON.stringify({
    ok,
    requestId,
    sessionBefore,
    socketResult,
    saveResult,
    sessionAfterSave,
    abnormalResult,
    submitResult,
    sessionAfterSubmit,
    scoreRows,
    detailRows,
    abnormalRows
  }, null, 2));

  if (!ok) {
    process.exit(1);
  }
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
"@
node -e $script