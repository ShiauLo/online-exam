export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'https://dev-api.exam-system.com';
export const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL ?? 'wss://dev-realtime.exam-system.com';
export const ISSUE_WS_BASE_URL = import.meta.env.VITE_ISSUE_WS_BASE_URL ?? WS_BASE_URL;
export const WS_PATH = import.meta.env.VITE_WS_PATH ?? '/socket.io';
export const ISSUE_WS_PATH = import.meta.env.VITE_ISSUE_WS_PATH ?? '/issue-socket.io';
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';
