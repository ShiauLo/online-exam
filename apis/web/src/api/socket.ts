import { io, type Socket } from 'socket.io-client';
import { ISSUE_WS_BASE_URL, ISSUE_WS_PATH, WS_BASE_URL, WS_PATH, USE_MOCK } from '@/config/runtime';
import { useAuthStore } from '@/stores/auth';

type Listener = (payload: unknown) => void;
type IssueEventName = 'issueNotify' | 'processNotify';
type ConnectionEventName = 'connected' | 'disconnect' | 'reconnect' | 'connect_error';

interface MockConnectionPayload {
  examId?: string;
  scope?: string;
  message?: string;
  reason?: string;
}

class MockSocketClient {
  private listeners = new Map<string, Set<Listener>>();
  private timer: number | null = null;
  private remainSeconds = 0;
  private currentExamId = '';

  constructor() {
    mockExamClients.add(this);
  }

  on(event: string, callback: Listener) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)?.add(callback);
  }

  off(event: string, callback?: Listener) {
    if (!callback) {
      this.listeners.delete(event);
      return;
    }
    this.listeners.get(event)?.delete(callback);
  }

  emit(event: string, payload: Record<string, unknown>) {
    if (event === 'enterExam') {
      this.currentExamId = String(payload.examId ?? this.currentExamId);
      const payloadRemainSeconds = Number(payload.remainSeconds);
      this.remainSeconds =
        Number.isFinite(payloadRemainSeconds) && payloadRemainSeconds > 0
          ? Math.floor(payloadRemainSeconds)
          : Number(payload.duration ?? 90) * 60;
      this.startCountdown(this.currentExamId);
    }

    if (event === 'reportScreen') {
      this.dispatch('processNotify', {
        issueId: `notify-${Date.now()}`,
        status: 'warning',
        processDesc: `检测到切屏 ${payload.screenOutCount ?? 0} 次`
      });
    }
  }

  disconnect() {
    this.stopCountdown();
    this.listeners.clear();
    mockExamClients.delete(this);
  }

  receiveConnectionEvent(event: ConnectionEventName, payload: MockConnectionPayload = {}) {
    if (event === 'disconnect') {
      this.stopCountdown();
      this.dispatch('disconnect', {
        examId: payload.examId ?? this.currentExamId,
        reason: payload.reason ?? 'mock disconnect'
      });
      return;
    }

    if (event === 'connect_error') {
      this.stopCountdown();
      this.dispatch('connect_error', {
        examId: payload.examId ?? this.currentExamId,
        message: payload.message ?? 'mock connect error'
      });
      return;
    }

    if (event === 'reconnect') {
      this.dispatch('reconnect', {
        examId: payload.examId ?? this.currentExamId
      });
      if (this.currentExamId) {
        this.dispatch('connected', {
          examId: payload.examId ?? this.currentExamId
        });
      }
      return;
    }

    this.dispatch('connected', {
      examId: payload.examId ?? this.currentExamId
    });
  }

  private startCountdown(examId: string) {
    this.stopCountdown();
    this.dispatch('connected', { examId });
    this.dispatch('countdown', {
      examId,
      remainTime: this.remainSeconds
    });
    this.timer = window.setInterval(() => {
      if (this.remainSeconds > 0) {
        this.remainSeconds -= 1;
      }
      this.dispatch('countdown', {
        examId,
        remainTime: this.remainSeconds
      });
    }, 1000);
  }

  private stopCountdown() {
    if (this.timer) {
      window.clearInterval(this.timer);
      this.timer = null;
    }
  }

  private dispatch(event: string, payload: unknown) {
    this.listeners.get(event)?.forEach((callback) => callback(payload));
  }
}

class MockIssueSocketClient {
  private listeners = new Map<string, Set<Listener>>();
  private subscribePayload: Record<string, unknown> = { scope: 'issue' };

  constructor() {
    mockIssueClients.add(this);
    window.setTimeout(() => {
      this.dispatch('connected', this.subscribePayload);
    }, 0);
  }

  on(event: string, callback: Listener) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    this.listeners.get(event)?.add(callback);
  }

  off(event: string, callback?: Listener) {
    if (!callback) {
      this.listeners.delete(event);
      return;
    }
    this.listeners.get(event)?.delete(callback);
  }

  emit(event: string, payload: Record<string, unknown>) {
    if (event === 'subscribeIssue') {
      this.subscribePayload = payload;
      this.dispatch('connected', payload);
    }
  }

  disconnect() {
    mockIssueClients.delete(this);
    this.listeners.clear();
  }

  receive(event: IssueEventName, payload: Record<string, unknown>) {
    this.dispatch(event, payload);
  }

  receiveConnectionEvent(event: ConnectionEventName, payload: MockConnectionPayload = {}) {
    if (event === 'disconnect') {
      this.dispatch('disconnect', {
        scope: payload.scope ?? this.subscribePayload.scope ?? 'issue',
        reason: payload.reason ?? 'mock disconnect'
      });
      return;
    }

    if (event === 'connect_error') {
      this.dispatch('connect_error', {
        scope: payload.scope ?? this.subscribePayload.scope ?? 'issue',
        message: payload.message ?? 'mock connect error'
      });
      return;
    }

    if (event === 'reconnect') {
      const reconnectPayload = {
        scope: payload.scope ?? this.subscribePayload.scope ?? 'issue'
      };
      this.dispatch('reconnect', reconnectPayload);
      this.dispatch('connected', reconnectPayload);
      return;
    }

    this.dispatch('connected', {
      scope: payload.scope ?? this.subscribePayload.scope ?? 'issue'
    });
  }

  private dispatch(event: string, payload: unknown) {
    this.listeners.get(event)?.forEach((callback) => callback(payload));
  }
}

const mockExamClients = new Set<MockSocketClient>();
const mockIssueClients = new Set<MockIssueSocketClient>();

export function createExamSocket() {
  if (USE_MOCK) {
    return new MockSocketClient();
  }
  const authStore = useAuthStore();
  return io(WS_BASE_URL, {
    path: WS_PATH,
    auth: {
      token: authStore.tokens?.accessToken ?? ''
    }
  }) as unknown as Socket;
}

export function createIssueSocket() {
  if (USE_MOCK) {
    return new MockIssueSocketClient();
  }
  const authStore = useAuthStore();
  return io(ISSUE_WS_BASE_URL, {
    path: ISSUE_WS_PATH,
    auth: {
      token: authStore.tokens?.accessToken ?? ''
    }
  }) as unknown as Socket;
}

export function emitMockIssueEvent(event: IssueEventName, payload: Record<string, unknown>) {
  if (!USE_MOCK) {
    return;
  }
  mockIssueClients.forEach((client) => client.receive(event, payload));
}

export function emitMockIssueConnectionEvent(event: ConnectionEventName, payload: MockConnectionPayload = {}) {
  if (!USE_MOCK) {
    return;
  }
  mockIssueClients.forEach((client) => client.receiveConnectionEvent(event, payload));
}

export function emitMockExamConnectionEvent(event: ConnectionEventName, payload: MockConnectionPayload = {}) {
  if (!USE_MOCK) {
    return;
  }
  mockExamClients.forEach((client) => client.receiveConnectionEvent(event, payload));
}
