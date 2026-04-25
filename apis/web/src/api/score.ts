import { request } from './http';
import type { PageResult } from '@/types/api';

export function queryScores(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/score/query',
    method: 'POST',
    data: payload
  });
}

export function queryScoreDetail(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/score/detail',
    method: 'POST',
    data: payload
  });
}

export function applyScoreRecheck(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/score/apply-recheck',
    method: 'POST',
    data: payload
  });
}

export function manualScore(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/score/manual-score',
    method: 'PUT',
    data: payload
  });
}

export function publishScores(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/score/publish',
    method: 'PUT',
    data: payload
  });
}

export function analyzeScores(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/score/analyze',
    method: 'POST',
    data: payload
  });
}

export function exportScores(params: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/score/export',
    method: 'GET',
    params
  });
}

export function handleAppeal(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/score/handle-appeal',
    method: 'PUT',
    data: payload
  });
}
