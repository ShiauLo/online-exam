import { afterEach, describe, expect, it } from 'vitest';
import { createApp } from '../app.js';
import { TokenResolver } from '../auth.js';
import type { AppConfig } from '../types.js';
import { createAppError, ErrorCatalog } from '../errors.js';

const config: AppConfig = {
  app: {
    name: 'exam-realtime',
    profiles: 'dev,local'
  },
  server: {
    host: '127.0.0.1',
    port: 8090,
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
  }
};

describe('realtimeRoutes', () => {
  function buildApp() {
    return createApp({
      config,
      tokenResolver: new TokenResolver(config.jwt.secret),
      services: {
        sessionService: {
          async loadSession() {
            return {
              exam: {
                examId: 8801,
                examName: 'Java 阶段测验',
                status: 'UNDERWAY',
                duration: 90,
                remainSeconds: 120,
                startTime: new Date().toISOString(),
                endTime: new Date(Date.now() + 120000).toISOString()
              },
              questions: [],
              answers: {
                '1001': 'A'
              },
              currentQId: '1001',
              screenOutCount: 1
            };
          }
        },
        progressService: {
          async saveProgress() {
            return {
              saved: true,
              savedAt: new Date().toISOString(),
              currentQId: '1001',
              screenOutCount: 2
            };
          },
          async syncScreenOutCount() {
            return 2;
          }
        },
        submitService: {
          async submit() {
            return {
              examId: 8801,
              studentId: 4001,
              status: 'SCORED',
              submittedAt: new Date().toISOString(),
              answeredCount: 10
            };
          }
        },
        abnormalService: {
          async report() {
            return {
              abnormalId: '1',
              reportedAt: new Date().toISOString()
            };
          }
        }
      }
    });
  }

  it('session 接口返回统一成功结构', async () => {
    const app = buildApp();
    const response = await app.inject({
      method: 'POST',
      url: '/api/exam/realtime/session',
      headers: {
        'X-User-Id': '4001',
        'X-Role-Id': '4',
        'X-Request-Id': 'req-1'
      },
      payload: {
        examId: 8801
      }
    });

    expect(response.statusCode).toBe(200);
    const payload = response.json();
    expect(payload.code).toBe(200);
    expect(payload.data.exam.examId).toBe(8801);
    expect(payload.data.answers['1001']).toBe('A');
    await app.close();
  });

  it('参数错误会返回结构化 400', async () => {
    const app = buildApp();
    const response = await app.inject({
      method: 'POST',
      url: '/api/exam/realtime/session',
      headers: {
        'X-User-Id': '4001',
        'X-Role-Id': '4',
        'X-Request-Id': 'req-2'
      },
      payload: {
        examId: 'bad'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json().code).toBe(4000);
    await app.close();
  });
});
