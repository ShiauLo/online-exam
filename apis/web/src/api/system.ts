import { request } from './http';
import type { PageResult } from '@/types/api';
import type { PermissionPayload } from '@/types/permission';

export function queryPermissions(payload: Record<string, unknown>) {
  return request<PermissionPayload>({
    url: '/api/system/permission/query',
    method: 'POST',
    data: payload
  });
}

export function queryConfigs(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/system/config/query',
    method: 'POST',
    data: payload
  });
}

export function updateConfig(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/system/config/update',
    method: 'PUT',
    data: payload
  });
}

export function queryAlarms(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/system/alarm/query',
    method: 'POST',
    data: payload
  });
}

export function updateAlarmSetting(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/system/alarm/setting',
    method: 'PUT',
    data: payload
  });
}

export function queryLogs(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/system/log/query',
    method: 'POST',
    data: payload
  });
}

export function exportLogs(params: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/system/log/export',
    method: 'GET',
    params
  });
}

export function queryBackups(payload: Record<string, unknown>) {
  return request<PageResult<Record<string, unknown>>>({
    url: '/api/system/data/query',
    method: 'POST',
    data: payload
  });
}

export function runBackup(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/system/data/backup',
    method: 'POST',
    data: payload
  });
}

export function restoreBackup(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/system/data/restore',
    method: 'POST',
    data: payload
  });
}

export function saveRoleTemplate(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/system/role',
    method: payload.roleId ? 'PUT' : 'POST',
    data: payload
  });
}

export function assignPermission(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/system/permission/assign',
    method: 'PUT',
    data: payload
  });
}
