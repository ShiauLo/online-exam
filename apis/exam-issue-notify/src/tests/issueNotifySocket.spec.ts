import type { AddressInfo } from 'node:net';
import { afterEach, describe, expect, it } from 'vitest';
import jwt from 'jsonwebtoken';
import { io as createClient } from 'socket.io-client';
import { createApp } from '../app.js';
import { TokenResolver } from '../auth.js';
import type { AppConfig } from '../types.js';
import { IssueNotifySocket } from '../socket/issueNotifySocket.js';

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
    pollIntervalMs: 20,
    historyLookbackMinutes: 1440
  }
};

describe('IssueNotifySocket', () => {
  let socketServer: IssueNotifySocket | null = null;

  afterEach(() => {
    socketServer?.close();
    socketServer = null;
  });

  it('subscribeIssue 后会先收到 connected，再收到 processNotify', async () => {
    let pollCount = 0;
    const app = createApp({
      config,
      tokenResolver: new TokenResolver(config.jwt.secret),
      services: {
        issueNotifyService: {
          resolveSubscriptionCursor() {
            return '2026-04-22T12:00:00.000Z';
          },
          async pullNotifications() {
            return {
              scope: 'issue' as const,
              cursor: '2026-04-22T12:00:00.000Z',
              notifications: []
            };
          },
          async pollNotifications() {
            pollCount += 1;
            if (pollCount === 1) {
              return {
                scope: 'issue' as const,
                cursor: '2026-04-22T12:00:02.000Z',
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
                    time: '2026-04-22T12:00:02.000Z'
                  }
                ]
              };
            }
            return {
              scope: 'issue' as const,
              cursor: '2026-04-22T12:00:02.000Z',
              notifications: []
            };
          }
        }
      }
    });

    await app.listen({
      host: '127.0.0.1',
      port: 0
    });

    socketServer = new IssueNotifySocket(app.server, {
      config,
      tokenResolver: new TokenResolver(config.jwt.secret),
      issueNotifyService: {
        resolveSubscriptionCursor() {
          return '2026-04-22T12:00:00.000Z';
        },
        async pullNotifications() {
          return {
            scope: 'issue' as const,
            cursor: '2026-04-22T12:00:00.000Z',
            notifications: []
          };
        },
        async pollNotifications() {
          pollCount += 1;
          if (pollCount === 1) {
            return {
              scope: 'issue' as const,
              cursor: '2026-04-22T12:00:02.000Z',
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
                  time: '2026-04-22T12:00:02.000Z'
                }
              ]
            };
          }
          return {
            scope: 'issue' as const,
            cursor: '2026-04-22T12:00:02.000Z',
            notifications: []
          };
        }
      }
    });

    const port = (app.server.address() as AddressInfo).port;
    const token = jwt.sign(
      {
        userId: 4001,
        roleId: 4
      },
      config.jwt.secret
    );

    await new Promise<void>((resolve, reject) => {
      const client = createClient(`http://127.0.0.1:${port}`, {
        path: config.server.socketPath,
        auth: {
          token
        },
        transports: ['websocket']
      });

      let connected = false;
      client.on('connected', () => {
        connected = true;
      });
      client.on('processNotify', (payload) => {
        try {
          expect(connected).toBe(true);
          expect(payload.issueId).toBe('9001');
          expect(payload.processDesc).toBe('教师已接单');
          client.disconnect();
          resolve();
        } catch (error) {
          reject(error);
        }
      });
      client.on('connect', () => {
        client.emit('subscribeIssue', {
          scope: 'issue'
        });
      });
      client.on('connect_error', reject);
    });

    await app.close();
  });
});
