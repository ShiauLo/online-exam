import { request } from './http';

export function fetchExamRealtimeSession(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/exam/realtime/session',
    method: 'POST',
    data: payload
  });
}

export function saveExamProgress(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/exam/realtime/save-progress',
    method: 'POST',
    data: payload
  });
}

export function submitExam(payload: Record<string, unknown>) {
  return request<Record<string, unknown>>({
    url: '/api/exam/realtime/submit',
    method: 'PUT',
    data: payload
  });
}

export function reportExamAbnormal(payload: Record<string, unknown>) {
  return request<boolean>({
    url: '/api/exam/realtime/report-abnormal',
    method: 'POST',
    data: payload
  });
}
