import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';
import { loadAppConfig } from '../config/loadConfig.js';

describe('loadAppConfig', () => {
  const tempDirs: string[] = [];

  afterEach(() => {
    tempDirs.splice(0).forEach((dir) => fs.rmSync(dir, { recursive: true, force: true }));
  });

  it('按 yaml -> dev yaml -> local properties -> env 顺序覆盖配置', () => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), 'exam-realtime-config-'));
    tempDirs.push(root);
    fs.mkdirSync(path.join(root, 'config'), { recursive: true });
    fs.writeFileSync(
      path.join(root, 'config', 'application.yaml'),
      [
        'app:',
        '  name: exam-realtime',
        '  profiles: dev,local',
        'server:',
        '  host: 0.0.0.0',
        '  port: 8090',
        '  socketPath: /socket.io',
        '  issueSocketPath: /issue-socket.io',
        '  corsOrigins: "*"',
        'mysql:',
        '  host: yaml-host',
        '  port: 3306',
        '  database: base_db',
        '  user: root',
        '  password: base',
        '  poolSize: 5',
        'redis:',
        '  host: 127.0.0.1',
        '  port: 6379',
        '  database: 0',
        '  password: ""',
        'nacos:',
        '  enabled: true',
        '  serverAddr: 127.0.0.1:8848',
        '  username: nacos',
        '  password: nacos',
        '  namespace: public',
        '  group: DEFAULT_GROUP',
        '  serviceIp: 127.0.0.1',
        '  servicePort: 8090',
        '  heartbeatIntervalMs: 5000',
        'downstream:',
        '  examCoreBaseUrl: http://127.0.0.1:8086',
        'jwt:',
        '  secret: mySuperSecretKeyThatIsAtLeast32BytesLongForHS512',
        'realtime:',
        '  ttlHoursAfterExam: 24',
        '  autoSubmitLockSeconds: 120',
        '  countdownIntervalMs: 1000',
        'notify:',
        '  defaultLimit: 20',
        '  maxLimit: 100',
        '  pollIntervalMs: 3000',
        '  historyLookbackMinutes: 1440'
      ].join('\n')
    );
    fs.writeFileSync(
      path.join(root, 'config', 'application-dev.yaml'),
      'mysql:\n  host: dev-host\n  database: dev_db\ndownstream:\n  examCoreBaseUrl: http://127.0.0.1:9000\n'
    );
    fs.writeFileSync(
      path.join(root, 'config', 'application-local.properties'),
      'mysql.host=local-host\nmysql.database=local_db\n'
    );

    const config = loadAppConfig(root, {
      EXAM_DB_HOST: 'env-host'
    });

    expect(config.mysql.host).toBe('env-host');
    expect(config.mysql.database).toBe('local_db');
    expect(config.downstream.examCoreBaseUrl).toBe('http://127.0.0.1:9000');
  });
});
