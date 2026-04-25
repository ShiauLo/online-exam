import { afterEach, describe, expect, it } from 'vitest';
import { createApp } from '../app.js';
import { TokenResolver } from '../auth.js';
import type { AppConfig } from '../types.js';

const config: AppConfig = {
  app: {
    name: 'exam-realtime',
    profiles: 'dev,local'
  },
  server: {
    host: '127.0.0.1',
    port: 8090,
    socketPath: '/socket.io',
    issueSocketPath: '/issue-socket.io',
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
  redis: {
    host: '127.0.0.1',
    port: 6379,
    database: 0,
    password: ''
  },
  nacos: {
    enabled: false,
    serverAddr: '127.0.0.1:8848',
    username: 'nacos',
    password: '',
    namespace: 'public',
    group: 'DEFAULT_GROUP',
    serviceIp: '127.0.0.1',
    servicePort: 8090,
    heartbeatIntervalMs: 5000
  },
  downstream: {
    examCoreBaseUrl: 'http://127.0.0.1:8086'
  },
  jwt: {
    secret: 'mySuperSecretKeyThatIsAtLeast32BytesLongForHS512'
  },
  realtime: {
    ttlHoursAfterExam: 24,
    autoSubmitLockSeconds: 120,
    countdownIntervalMs: 1000
  },
  notify: {
    defaultLimit: 20,
    maxLimit: 100,
    pollIntervalMs: 30,
    historyLookbackMinutes: 1440
  }
};

describe('issueNotifyRoutes', () => {
  function buildApp() {
    return createApp({
      config,
      tokenResolver: new TokenResolver(config.jwt.secret),
      services: {
        sessionService: {
          async loadSession() {
            throw new Error('unused');
          }
        },
        progressService: {
          async saveProgress() {
            throw new Error('unused');
          },
          async syncScreenOutCount() {
            return 0;
          }
        },
        submitService: {
          async submit() {
            throw new Error('unused');
          }
        },
        abnormalService: {
          async report() {
            throw new Error('unused');
          }
        },
        issueNotifyService: {
          resolveSubscriptionCursor(cursor?: string) {
            return cursor ?? '2026-04-22T00:00:00.000Z';
          },
          async pullNotifications() {
            return {
              scope: 'issue' as const,
              cursor: '2026-04-22T12:00:00.000Z',
              notifications: [
                {
                  eventId: 'issue-process-1',
                  eventType: 'processNotify' as const,
                  issueId: '9001',
                  title: '考试页面卡顿',
                  type: 'EXAM',
                  status: 'PROCESSING',
                  handlerId: 3001,
                  handlerName: '张老师',
                  reporterId: 4001,
                  reporterName: '学生A',
                  processDesc: '教师已接单',
                  action: 'handled' as const,
                  time: '2026-04-22T12:00:00.000Z'
                }
              ]
            };
          },
          async pollNotifications() {
            return {
              scope: 'issue' as const,
              cursor: '2026-04-22T12:00:00.000Z',
              notifications: []
            };
          }
        }
      }
    });
  }

  afterEach(async () => {
    // 预留给单测资源回收
  });

  it('通知查询接口返回统一成功结构', async () => {
    const app = buildApp();
    const response = await app.inject({
      method: 'POST',
      url: '/api/issue/notify',
      headers: {
        'X-User-Id': '4001',
        'X-Role-Id': '4',
        'X-Request-Id': 'req-issue-1'
      },
      payload: {
        limit: 10
      }
    });

    expect(response.statusCode).toBe(200);
    const payload = response.json();
    expect(payload.code).toBe(200);
    expect(payload.data.scope).toBe('issue');
    expect(payload.data.notifications[0].eventType).toBe('processNotify');
    await app.close();
  });

  it('参数错误会返回结构化 400', async () => {
    const app = buildApp();
    const response = await app.inject({
      method: 'POST',
      url: '/api/issue/notify',
      headers: {
        'X-User-Id': '4001',
        'X-Role-Id': '4',
        'X-Request-Id': 'req-issue-2'
      },
      payload: {
        cursor: 'bad-cursor'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json().code).toBe(4000);
    await app.close();
  });
});
