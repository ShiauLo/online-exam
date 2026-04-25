import axios, { AxiosError, type AxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { API_BASE_URL, USE_MOCK } from '@/config/runtime';
import type { ApiErrorPayload, ApiResponse } from '@/types/api';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';

declare module 'axios' {
  interface AxiosRequestConfig {
    skipAuth?: boolean;
    skipRefresh?: boolean;
    _retry?: boolean;
  }
}

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

let refreshPromise: Promise<void> | null = null;

export function normalizeResponseData<T>(data: T): T {
  if (!data || typeof data !== 'object' || Array.isArray(data)) {
    return data;
  }

  const candidate = data as Record<string, unknown>;
  if (Array.isArray(candidate.records) && !Array.isArray(candidate.list)) {
    return {
      ...candidate,
      list: candidate.records
    } as T;
  }

  return data;
}

async function loadMockModule() {
  return import('@/mock');
}

http.interceptors.request.use((config) => {
  const authStore = useAuthStore();
  if (!config.skipAuth && authStore.tokens?.accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${authStore.tokens.accessToken}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    if (payload?.requestId) {
      useAppStore().setLastRequestId(payload.requestId);
    }
    return response;
  },
  async (error: AxiosError<ApiErrorPayload>) => {
    const authStore = useAuthStore();
    const config = error.config;

    if (
      error.response?.status === 401 &&
      config &&
      !config._retry &&
      !config.skipRefresh &&
      authStore.tokens?.refreshToken
    ) {
      config._retry = true;
      refreshPromise = refreshPromise ?? authStore.refreshSession();

      try {
        await refreshPromise;
        refreshPromise = null;
        return http.request(config);
      } catch (refreshError) {
        refreshPromise = null;
        authStore.clearSession();
        return Promise.reject(refreshError);
      }
    }

    if (error.response?.data?.requestId) {
      useAppStore().setLastRequestId(error.response.data.requestId);
    }

    ElMessage.error(error.response?.data?.msg ?? '请求失败，请稍后重试');
    return Promise.reject(error);
  }
);

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  if (USE_MOCK) {
    const { handleMockRequest, MockHttpError } = await loadMockModule();
    try {
      const authStore = useAuthStore();
      const response = (await handleMockRequest<T>({
        ...config,
        headers: {
          ...(config.headers ?? {}),
          Authorization: config.skipAuth
            ? ''
            : `Bearer ${authStore.tokens?.accessToken ?? ''}`
        }
      })) as ApiResponse<T>;

      useAppStore().setLastRequestId(response.requestId);
      return normalizeResponseData(response.data);
    } catch (error) {
      if (error instanceof MockHttpError) {
        useAppStore().setLastRequestId(error.payload.requestId);
        if (error.status === 401 && !config.skipRefresh && !config._retry) {
          const authStore = useAuthStore();
          if (authStore.tokens?.refreshToken) {
            config._retry = true;
            refreshPromise = refreshPromise ?? authStore.refreshSession();
            try {
              await refreshPromise;
              refreshPromise = null;
              return request<T>(config);
            } catch (refreshError) {
              refreshPromise = null;
              authStore.clearSession();
              throw refreshError;
            }
          }
        }
        ElMessage.error(error.payload.msg);
      }
      throw error;
    }
  }

  const response = await http.request<ApiResponse<T>>(config);
  return normalizeResponseData(response.data.data);
}
