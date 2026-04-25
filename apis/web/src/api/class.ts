import { request } from './http';
import type { PageResult } from '@/types/api';

export function queryClasses(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/class/query',
    method: 'POST',
    data: payload
  });
}

export function createClass(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/class/create',
    method: 'POST',
    data: payload
  });
}

export function updateClass(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/class/update',
    method: 'PUT',
    data: payload
  });
}

export function deleteClass(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/class/delete',
    method: 'DELETE',
    data: payload
  });
}

export function applyJoinClass(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/class/apply-join',
    method: 'POST',
    data: payload
  });
}

export function approveJoinClass(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/class/approve-join',
    method: 'PUT',
    data: payload
  });
}

export function removeStudent(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/class/remove-student',
    method: 'PUT',
    data: payload
  });
}

export function quitClass(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/class/quit',
    method: 'PUT',
    data: payload
  });
}

export function importClasses(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/class/import',
    method: 'POST',
    data: payload
  });
}
