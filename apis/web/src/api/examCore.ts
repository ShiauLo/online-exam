import { request } from './http';
import type { PageResult } from '@/types/api';

export function queryExams(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/exam/core/query',
    method: 'POST',
    data: payload
  });
}

export function createExam(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/exam/core/create',
    method: 'POST',
    data: payload
  });
}

export function updateExamParams(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/exam/core/update/params',
    method: 'PUT',
    data: payload
  });
}

export function distributeExam(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/exam/core/distribute',
    method: 'PUT',
    data: payload
  });
}

export function toggleExamStatus(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/exam/core/toggle-status',
    method: 'PUT',
    data: payload
  });
}

export function approveRetest(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/exam/core/approve-retest',
    method: 'PUT',
    data: payload
  });
}
