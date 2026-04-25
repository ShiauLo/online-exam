[CmdletBinding()]
param(
    [string]$ApiBaseUrl = "http://127.0.0.1:8080",
    [int]$ExamId = 8801,
    [int]$ClassId = 501,
    [int]$StudentId = 4,
    [int]$TeacherId = 3
)

$ErrorActionPreference = "Stop"

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "=== $Title ==="
}

Write-Section "执行问题通知主链"
$script = @"
const jwt = require('./exam-realtime/node_modules/jsonwebtoken');

const secret = 'mySuperSecretKeyThatIsAtLeast32BytesLongForHS512';
const studentToken = jwt.sign({ userId: $StudentId, roleId: 4 }, secret, { subject: '$StudentId', expiresIn: '30m' });
const teacherToken = jwt.sign({ userId: $TeacherId, roleId: 3 }, secret, { subject: '$TeacherId', expiresIn: '30m' });

function headers(token, requestId) {
  return {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token,
    'X-Request-Id': requestId
  };
}

async function call(method, url, token, body, requestId) {
  const response = await fetch(url, {
    method,
    headers: headers(token, requestId),
    body: JSON.stringify(body ?? {})
  });
  const text = await response.text();
  return { status: response.status, json: JSON.parse(text) };
}

(async () => {
  const requestId = 'verify-live-issue-' + Date.now();
  const teacherCursor = new Date(Date.now() - 2 * 60 * 1000).toISOString();
  const studentCursor = teacherCursor;

  const createResult = await call('POST', '$ApiBaseUrl/api/issue/core/create', studentToken, {
    type: 'EXAM',
    title: '联调验收问题-' + requestId,
    desc: '用于验证真实问题通知主链。',
    examId: $ExamId,
    classId: $ClassId
  }, requestId);
  const issueId = String(createResult.json.data.issueId);

  await new Promise((resolve) => setTimeout(resolve, 3500));
  const teacherNotify = await call('POST', '$ApiBaseUrl/api/issue/notify', teacherToken, {
    cursor: teacherCursor,
    limit: 20
  }, requestId);

  const handleResult = await call('PUT', '$ApiBaseUrl/api/issue/core/handle', teacherToken, {
    issueId,
    result: '已受理',
    solution: '已记录并进入核查流程'
  }, requestId);

  await new Promise((resolve) => setTimeout(resolve, 3500));
  const studentNotify = await call('POST', '$ApiBaseUrl/api/issue/notify', studentToken, {
    cursor: studentCursor,
    limit: 20
  }, requestId);
  const trackResult = await call('POST', '$ApiBaseUrl/api/issue/core/track', studentToken, {
    issueId
  }, requestId);

  const teacherEvent = teacherNotify.json.data.notifications.find((item) => item.issueId === issueId && item.eventType === 'issueNotify');
  const studentEvent = studentNotify.json.data.notifications.find((item) => item.issueId === issueId && item.eventType === 'processNotify');
  const actions = trackResult.json.data.logs.map((item) => item.action);

  const ok =
    createResult.status === 200 &&
    teacherNotify.status === 200 &&
    handleResult.status === 200 &&
    studentNotify.status === 200 &&
    trackResult.status === 200 &&
    !!teacherEvent &&
    !!studentEvent &&
    actions.includes('CREATED') &&
    actions.includes('HANDLED');

  console.log(JSON.stringify({
    ok,
    requestId,
    createResult,
    teacherEvent,
    handleResult,
    studentEvent,
    trackResult,
    actions
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