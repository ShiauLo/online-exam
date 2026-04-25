import { request } from './http';
import type { AuthTokens, LoginPayload, LoginResult, UserProfile } from '@/types/auth';

export function login(payload: LoginPayload) {
  return request<LoginResult>({
    url: '/api/account/login',
    method: 'POST',
    skipAuth: true,
    skipRefresh: true,
    data: payload
  });
}

export function refreshToken(refreshToken: string) {
  return request<AuthTokens | string>({
    url: '/api/account/refresh-token',
    method: 'POST',
    skipRefresh: true,
    data: { refreshToken }
  });
}

export function logout(payload: { userId: string; refreshToken: string }) {
  return request<boolean>({
    url: '/api/account/logout',
    method: 'POST',
    data: payload
  });
}

export function sendSmsCode(phone: string) {
  return request<boolean>({
    url: '/api/account/send/verifycode',
    method: 'POST',
    skipAuth: true,
    skipRefresh: true,
    data: { phoneNumber: phone }
  });
}

export function registerStudent(payload: Record<string, unknown>) {
  return request<UserProfile>({
    url: '/api/account/create',
    method: 'POST',
    skipAuth: true,
    skipRefresh: true,
    data: payload
  });
}
