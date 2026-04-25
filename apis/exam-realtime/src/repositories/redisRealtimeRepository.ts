import type { DraftState } from '../types.js';

interface RedisLikeClient {
  get(key: string): Promise<string | null>;
  set: (...args: any[]) => Promise<any>;
  del: (...args: any[]) => Promise<number>;
}

const defaultDraftState: DraftState = {
  answers: {},
  screenOutCount: 0
};

function draftKey(examId: number, studentId: number) {
  return `exam:realtime:draft:${examId}:${studentId}`;
}

function socketKey(examId: number, studentId: number) {
  return `exam:realtime:socket:${examId}:${studentId}`;
}

function submitLockKey(examId: number, studentId: number) {
  return `exam:realtime:submit-lock:${examId}:${studentId}`;
}

export class RedisRealtimeRepository {
  constructor(private readonly client: RedisLikeClient) {}

  async getDraftState(examId: number, studentId: number): Promise<DraftState> {
    const raw = await this.client.get(draftKey(examId, studentId));
    if (!raw) {
      return { ...defaultDraftState };
    }
    try {
      const parsed = JSON.parse(raw) as DraftState;
      return {
        answers: parsed.answers ?? {},
        currentQId: parsed.currentQId,
        lastSavedAt: parsed.lastSavedAt,
        screenOutCount: parsed.screenOutCount ?? 0
      };
    } catch (_error) {
      return { ...defaultDraftState };
    }
  }

  async saveDraftState(examId: number, studentId: number, state: DraftState, ttlSeconds: number) {
    await this.client.set(draftKey(examId, studentId), JSON.stringify(state), {
      EX: ttlSeconds
    });
  }

  async clearDraftState(examId: number, studentId: number) {
    await this.client.del(draftKey(examId, studentId));
  }

  async setActiveSocketId(examId: number, studentId: number, socketId: string, ttlSeconds: number) {
    await this.client.set(socketKey(examId, studentId), socketId, {
      EX: ttlSeconds
    });
  }

  async getActiveSocketId(examId: number, studentId: number) {
    return this.client.get(socketKey(examId, studentId));
  }

  async clearActiveSocketId(examId: number, studentId: number) {
    await this.client.del(socketKey(examId, studentId));
  }

  async acquireSubmitLock(examId: number, studentId: number, ttlSeconds: number) {
    const result = await this.client.set(submitLockKey(examId, studentId), '1', {
      NX: true,
      EX: ttlSeconds
    });
    return result === 'OK';
  }

  async releaseSubmitLock(examId: number, studentId: number) {
    await this.client.del(submitLockKey(examId, studentId));
  }
}
