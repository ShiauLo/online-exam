import { createServer } from 'node:http';
import jwt from 'jsonwebtoken';
import { afterEach, describe, expect, it } from 'vitest';
import { io as clientIo, type Socket } from 'socket.io-client';
import { ExamRealtimeSocket } from '../socket/examRealtimeSocket.js';
import type { AppConfig, RequestContext } from '../types.js';
import { TokenResolver } from '../auth.js';

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
    countdownIntervalMs: 50
  }
};

describe('ExamRealtimeSocket', () => {
  let httpServer = createServer();
  let socketServer: ExamRealtimeSocket | null = null;
  let client: Socket | null = null;

  afterEach(async () => {
    client?.disconnect();
    socketServer?.close();
    await new Promise<void>((resolve) => httpServer.close(() => resolve()));
    httpServer = createServer();
    socketServer = null;
    client = null;
  });

  it('enterExam 后会先收到 connected，再推送 countdown，并在倒计时结束后自动提交一次', async () => {
    await new Promise<void>((resolve) => httpServer.listen(0, '127.0.0.1', () => resolve()));
    const address = httpServer.address();
    const port = typeof address === 'object' && address ? address.port : 0;
    const autoSubmitCalls: number[] = [];
    const activeSockets = new Map<string, string>();

    socketServer = new ExamRealtimeSocket(httpServer, {
      config,
      tokenResolver: new TokenResolver(config.jwt.secret),
      sessionService: {
        async loadSession() {
          return {
            exam: {
              examId: 8801,
              examName: 'Java 阶段测验',
              status: 'UNDERWAY',
              duration: 90,
              remainSeconds: 1,
              startTime: new Date().toISOString(),
              endTime: new Date(Date.now() + 1000).toISOString()
            },
            questions: [],
            answers: {},
            screenOutCount: 0
          };
        }
      } as any,
      progressService: {
        async saveProgress() {
          throw new Error('unused');
        },
        async syncScreenOutCount() {
          return 1;
        }
      },
      submitService: {
        async submit() {
          throw new Error('unused');
        },
        async autoSubmit(examId: number, _context: RequestContext) {
          autoSubmitCalls.push(examId);
          return null;
        }
      } as any,
      redisRepository: {
        async getActiveSocketId(examId: number, studentId: number) {
          return activeSockets.get(`${examId}:${studentId}`) ?? null;
        },
        async setActiveSocketId(examId: number, studentId: number, socketId: string) {
          activeSockets.set(`${examId}:${studentId}`, socketId);
        },
        async clearActiveSocketId(examId: number, studentId: number) {
          activeSockets.delete(`${examId}:${studentId}`);
        }
      } as any
    });

    const token = jwt.sign(
      {
        userId: 4001,
        roleId: 4
      },
      config.jwt.secret
    );

    client = clientIo(`http://127.0.0.1:${port}`, {
      path: '/socket.io',
      transports: ['websocket'],
      auth: {
        token
      }
    });

    const events: string[] = [];
    await new Promise<void>((resolve, reject) => {
      client?.on('connect_error', (error) => {
        reject(error);
      });
      client?.on('connected', () => {
        events.push('connected');
      });
      client?.on('countdown', () => {
        events.push('countdown');
      });
      client?.on('connect', () => {
        client?.emit('enterExam', {
          examId: 8801
        });
      });
      setTimeout(() => {
        if (events.includes('connected') && events.includes('countdown') && autoSubmitCalls.length === 1) {
          resolve();
          return;
        }
        reject(new Error(`unexpected events: ${events.join(',')}`));
      }, 1000);
    });

    expect(events[0]).toBe('connected');
    expect(events).toContain('countdown');
    expect(autoSubmitCalls).toEqual([8801]);
  });
});
