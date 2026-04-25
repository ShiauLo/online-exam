import { request } from './http';
import type { PageResult } from '@/types/api';
import { emitMockIssueEvent } from './socket';

function resolveLatestProcess(issue: Record<string, unknown>) {
  const processList = Array.isArray(issue.process) ? issue.process : [];
  return (processList[0] ?? {}) as Record<string, unknown>;
}

function normalizeIssueType(type: unknown) {
  return String(type ?? '').trim().toUpperCase();
}

function actionLabel(action: unknown) {
  const normalized = String(action ?? '').trim().toUpperCase();
  if (normalized === 'CREATED') {
    return '问题已创建';
  }
  if (normalized === 'HANDLED') {
    return '问题已处理';
  }
  if (normalized === 'TRANSFERRED') {
    return '问题已转派';
  }
  if (normalized === 'CLOSED') {
    return '问题已关闭';
  }
  return '流程更新';
}

export function queryIssues(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/issue/core/query',
    method: 'POST',
    data: payload
  });
}

export async function createIssue(payload: Record<string, unknown>) {
  const result = await request<Record<string, unknown>>({
    url: '/api/issue/core/create',
    method: 'POST',
    data: {
      ...payload,
      type: normalizeIssueType(payload.type)
    }
  });
  const latestProcess = resolveLatestProcess(result);
  emitMockIssueEvent('issueNotify', {
    issueId: result.issueId,
    title: result.title,
    status: result.status,
    handlerName: result.handlerName,
    reporterName: result.reporterName,
    processDesc: latestProcess.desc,
    time: latestProcess.time,
    action: 'created'
  });
  return result;
}

export async function handleIssue(payload: Record<string, unknown>) {
  const result = await request<Record<string, unknown>>({
    url: '/api/issue/core/handle',
    method: 'PUT',
    data: payload
  });
  const latestProcess = resolveLatestProcess(result);
  emitMockIssueEvent('processNotify', {
    issueId: result.issueId,
    title: result.title,
    status: result.status,
    handlerName: result.handlerName,
    reporterName: result.reporterName,
    processDesc: latestProcess.desc,
    time: latestProcess.time,
    action: 'handled'
  });
  return result;
}

export async function transferIssue(payload: Record<string, unknown>) {
  const result = await request<Record<string, unknown>>({
    url: '/api/issue/core/transfer',
    method: 'PUT',
    data: payload
  });
  const latestProcess = resolveLatestProcess(result);
  emitMockIssueEvent('processNotify', {
    issueId: result.issueId,
    title: result.title,
    status: result.status,
    handlerName: result.handlerName,
    reporterName: result.reporterName,
    processDesc: latestProcess.desc,
    time: latestProcess.time,
    action: 'transferred'
  });
  return result;
}

export async function closeIssue(payload: Record<string, unknown>) {
  const result = await request<Record<string, unknown>>({
    url: '/api/issue/core/close',
    method: 'PUT',
    data: {
      ...payload,
      confirmResult: payload.confirmResult ?? 'CONFIRMED'
    }
  });
  const latestProcess = resolveLatestProcess(result);
  emitMockIssueEvent('processNotify', {
    issueId: result.issueId,
    title: result.title,
    status: result.status,
    handlerName: result.handlerName,
    reporterName: result.reporterName,
    processDesc: latestProcess.desc,
    time: latestProcess.time,
    action: 'closed'
  });
  return result;
}

export function trackIssue(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/issue/core/track',
    method: 'POST',
    data: payload
  }).then((result) => {
    const logs = Array.isArray(result.logs) ? result.logs : [];
    return logs.map((item) => ({
      time: String(item.occurredAt ?? '-'),
      title: actionLabel(item.action),
      desc: String(item.content ?? '无处理说明')
    }));
  });
}
