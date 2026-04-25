import { request } from './http';

export function downloadFile(params: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/resource/file/download',
    method: 'GET',
    params
  });
}
