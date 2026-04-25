import type { ApiResponse } from '@/types/api';
import {
  mockAlarms,
  mockBackups,
  mockLogs,
  mockRoleTemplates,
  mockSystemConfigs
} from '../data';
import { fail, paginate, requireCurrentUser, success, type MockRequestContext } from './shared';

export function handleSystemMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data } = context;

  if (url === '/api/system/config/query' && method === 'POST') {
    return success(
      paginate(mockSystemConfigs, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/system/config/update' && method === 'PUT') {
    const record = mockSystemConfigs.find((item) => item.configKey === String(data.configKey ?? ''));
    if (!record) {
      fail(404, '配置项不存在');
    }
    record.configValue = String(data.configValue ?? record.configValue);
    return success(record);
  }

  if (url === '/api/system/alarm/query' && method === 'POST') {
    return success(
      paginate(mockAlarms, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/system/alarm/setting' && method === 'PUT') {
    const record = mockAlarms.find((item) => item.alarmType === String(data.alarmType ?? ''));
    if (!record) {
      fail(404, '告警不存在');
    }
    record.threshold = String(data.threshold ?? record.threshold);
    record.recipients = (data.recipients as string[]) ?? record.recipients;
    return success(record);
  }

  if (url === '/api/system/log/query' && method === 'POST') {
    const currentUser = requireCurrentUser(context.token);
    let list = [...mockLogs];
    if (currentUser.roleType === 'admin') {
      list = list.filter((item) => item.logType === 'business');
    }
    if (currentUser.roleType === 'ops') {
      list = list.filter((item) => item.logType === 'system');
    }
    if (data.logType) {
      list = list.filter((item) => item.logType === data.logType);
    }
    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/system/log/export' && method === 'GET') {
    return success({ fileKey: 'system-log-export-001' });
  }

  if (url === '/api/system/data/query' && method === 'POST') {
    return success(
      paginate(mockBackups, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/system/data/backup' && method === 'POST') {
    mockBackups.unshift({
      backupId: `b-${mockBackups.length + 1}`,
      backupType: String(data.backupType ?? 'incremental') as 'full' | 'incremental' | 'config' | 'audit',
      status: 'RUNNING',
      lifecycleStage: '排队中',
      updateTime: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      canRestore: false
    });
    return success(true);
  }

  if (url === '/api/system/data/restore' && method === 'POST') {
    if (String(data.verifyCode1 ?? '') !== '9527' || String(data.verifyCode2 ?? '') !== '3141') {
      fail(400, '双人验证码不正确');
    }
    return success({ restored: true });
  }

  if (url === '/api/system/role' && (method === 'POST' || method === 'PUT')) {
    const roleId = String(data.roleId ?? '');
    const record = mockRoleTemplates.find((item) => item.roleId === roleId);
    if (record) {
      record.roleName = String(data.roleName ?? record.roleName);
      record.permissionCount = Array.isArray(data.permissionIds)
        ? data.permissionIds.length
        : record.permissionCount;
      return success(record);
    }
    const created = {
      roleId: `role-${mockRoleTemplates.length + 1}`,
      roleName: String(data.roleName ?? '新角色'),
      permissionCount: Array.isArray(data.permissionIds) ? data.permissionIds.length : 0
    };
    mockRoleTemplates.unshift(created);
    return success(created);
  }

  if (url === '/api/system/permission/assign' && method === 'PUT') {
    return success(true);
  }

  return null;
}
