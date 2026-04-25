import { request } from './http';
import type { PageResult } from '@/types/api';

export function queryPapers(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/paper/query',
    method: 'POST',
    data: payload
  });
}

export function createManualPaper(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/paper/create/manual',
    method: 'POST',
    data: payload
  });
}

export function createAutoPaper(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/paper/create/auto',
    method: 'POST',
    data: payload
  });
}

export function publishPaper(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/paper/publish',
    method: 'PUT',
    data: payload
  });
}

export function terminatePaper(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/paper/terminate',
    method: 'PUT',
    data: payload
  });
}

export function recyclePaper(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/paper/recycle',
    method: 'PUT',
    data: payload
  });
}

export function exportPaper(params: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/paper/export',
    method: 'GET',
    params
  });
}
