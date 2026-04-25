import { request } from './http';
import type { PageResult } from '@/types/api';

export function queryQuestions(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/question/query',
    method: 'POST',
    data: payload
  });
}

export function queryCategories(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/question/category/query',
    method: 'POST',
    data: payload
  });
}

export function createQuestion(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/question/create',
    method: 'POST',
    data: payload
  });
}

export function updateQuestion(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/question/update',
    method: 'PUT',
    data: payload
  });
}

export function toggleQuestionStatus(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/question/toggle-status',
    method: 'PUT',
    data: payload
  });
}

export function importQuestions(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/question/import',
    method: 'POST',
    data: payload
  });
}

export function exportQuestions(params: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/question/export',
    method: 'GET',
    params
  });
}

export function auditQuestion(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/question/audit',
    method: 'PUT',
    data: payload
  });
}
