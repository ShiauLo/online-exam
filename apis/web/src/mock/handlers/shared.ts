import type { AxiosRequestConfig, Method } from 'axios';
import type { ApiErrorPayload, ApiResponse, PageResult } from '@/types/api';
import type { RoleType, UserProfile } from '@/types/auth';
import { buildRequestId, getUserByToken, mockUsers } from '../data';

export interface MockRequestConfig extends AxiosRequestConfig {
  method?: Method | string;
}

export interface MockRequestContext {
  method: string;
  url: string;
  data: Record<string, unknown>;
  params: Record<string, unknown>;
  token?: string;
}

export class MockHttpError extends Error {
  status: number;
  payload: ApiErrorPayload;

  constructor(status: number, payload: ApiErrorPayload) {
    super(payload.msg);
    this.status = status;
    this.payload = payload;
  }
}

export function success<T>(data: T): ApiResponse<T> {
  return {
    code: 200,
    msg: 'success',
    data,
    requestId: buildRequestId(),
    timestamp: Date.now()
  };
}

export function fail(status: number, msg: string, errors?: string[]): never {
  throw new MockHttpError(status, {
    code: status,
    msg,
    requestId: buildRequestId(),
    timestamp: Date.now(),
    errors
  });
}

export function paginate<T>(list: T[], pageNum = 1, pageSize = 10): PageResult<T> {
  const start = (pageNum - 1) * pageSize;
  return {
    list: list.slice(start, start + pageSize),
    total: list.length,
    pageNum,
    pageSize
  };
}

export function parseToken(headers?: Record<string, unknown>) {
  const authorization = headers?.Authorization ?? headers?.authorization;
  if (typeof authorization !== 'string') {
    return undefined;
  }
  return authorization.replace('Bearer ', '');
}

export function containsRole(roles: RoleType[], role: RoleType) {
  return roles.includes(role);
}

export function toUserProfile(user: typeof mockUsers[number]): UserProfile {
  const { password: _, status: __, ...profile } = user;
  return profile;
}

export function requireCurrentUser(token?: string) {
  const user = getUserByToken(token);
  if (!user) {
    fail(401, '登录态失效，请重新登录');
  }
  return user;
}

export function applyQueryFilter<T>(list: T[], keyword: string, getter: (item: T) => string[]) {
  if (!keyword.trim()) {
    return list;
  }

  return list.filter((item) => getter(item).some((value) => value?.includes(keyword)));
}

export function buildContext(config: MockRequestConfig): MockRequestContext {
  return {
    method: (config.method ?? 'GET').toUpperCase(),
    url: config.url ?? '',
    data: (config.data ?? {}) as Record<string, unknown>,
    params: (config.params ?? {}) as Record<string, unknown>,
    token: parseToken(config.headers as Record<string, unknown>)
  };
}
