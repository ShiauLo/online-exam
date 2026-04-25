import { describe, expect, it } from 'vitest';
import { DefaultIssueNotifyService } from '../services/issueNotifyService.js';
import type { AppConfig, IssueNotifyRepository, RequestContext } from '../types.js';

const config: AppConfig = {
  app: {
    name: 'exam-issue-notify',
    profiles: 'dev,local'
  },
  server: {
    host: '127.0.0.1',
    port: 8091,
    socketPath: '/socket.io',
    corsOrigins: '*'
  },
  mysql: {
    host: '127.0.0.1',
    port: 3306,
    database: 'online_exam_db',
    user: 'root',
    password: '',
    poolSize: 5
  },
  nacos: {
    enabled: false,
    serverAddr: '127.0.0.1:8848',
    username: 'nacos',
    password: '',
    namespace: 'public',
    group: 'DEFAULT_GROUP',
    serviceIp: '127.0.0.1',
    servicePort: 8091,
    heartbeatIntervalMs: 5000
  },
  jwt: {
    secret: 'mySuperSecretKeyThatIsAtLeast32BytesLongForHS512'
  },
  notify: {
    defaultLimit: 20,
    maxLimit: 100,
    pollIntervalMs: 30,
    historyLookbackMinutes: 1440
  }
};

const teacherContext: RequestContext = {
  userId: 3,
  roleId: 3,
  requestId: 'req-issue-notify-service'
};

function buildRepository(): IssueNotifyRepository {
  return {
    async listCreatedIssuesSince() {
      return [
        {
          issueId: '1001',
          type: 'EXAM',
          title: '最早的问题',
          status: 'PENDING',
          reporterId: 4,
          handlerId: null,
          teacherIds: [3],
          description: 'created-1',
          occurredAt: '2026-04-24T10:00:00.000Z'
        },
        {
          issueId: '1002',
          type: 'EXAM',
          title: '中间的问题',
          status: 'PENDING',
          reporterId: 4,
          handlerId: null,
          teacherIds: [3],
          description: 'created-2',
          occurredAt: '2026-04-24T11:00:00.000Z'
        },
        {
          issueId: '1003',
          type: 'EXAM',
          title: '最新的问题',
          status: 'PENDING',
          reporterId: 4,
          handlerId: null,
          teacherIds: [3],
          description: 'created-3',
          occurredAt: '2026-04-24T12:00:00.000Z'
        }
      ];
    },
    async listProcessUpdatesSince() {
      return [];
    }
  };
}

describe('issueNotifyService', () => {
  it('首次拉取无 cursor 时返回最近的通知窗口', async () => {
    const service = new DefaultIssueNotifyService(buildRepository(), config);

    const result = await service.pullNotifications({ limit: 2 }, teacherContext);

    expect(result.notifications).toHaveLength(2);
    expect(result.notifications.map((item) => item.issueId)).toEqual(['1002', '1003']);
    expect(result.cursor).toBe('2026-04-24T12:00:00.000Z');
  });

  it('增量轮询有 cursor 时保持从旧到新的顺序截断', async () => {
    const service = new DefaultIssueNotifyService(buildRepository(), config);

    const result = await service.pollNotifications('2026-04-24T09:30:00.000Z', teacherContext, 2);

    expect(result.notifications).toHaveLength(2);
    expect(result.notifications.map((item) => item.issueId)).toEqual(['1001', '1002']);
    expect(result.cursor).toBe('2026-04-24T11:00:00.000Z');
  });
});
