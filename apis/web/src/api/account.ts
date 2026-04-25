import { request } from './http';
import type { PageResult } from '@/types/api';
import type { UserProfile } from '@/types/auth';

export function updateProfile(payload: Record<string, unknown>) {
  return request<UserProfile>({
    url: '/api/account/update',
    method: 'PUT',
    data: payload
  });
}

export function queryAccounts(payload: Record<string, unknown>) {
  return request<PageResult<UserProfile>>({
    url: '/api/account/query',
    method: 'POST',
    data: payload
  });
}

export function queryAccountSummary(payload: Record<string, unknown>, accessToken?: string) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/account/query',
    method: 'POST',
    data: payload,
    headers: accessToken
      ? {
          Authorization: `Bearer ${accessToken}`
        }
      : undefined
  });
}

export function createAccount(payload: Record<string, unknown>) {
  return request<UserProfile>({
    url: '/api/account/create',
    method: 'POST',
    data: payload
  });
}

export function auditAccount(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/account/audit',
    method: 'PUT',
    data: payload
  });
}

export function freezeAccount(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/account/freeze',
    method: 'PUT',
    data: payload
  });
}

export function resetPassword(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/account/reset-password',
    method: 'PUT',
    data: payload
  });
}

export function queryLoginLogs(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/account/login-log/query',
    method: 'POST',
    data: payload
  });
}
