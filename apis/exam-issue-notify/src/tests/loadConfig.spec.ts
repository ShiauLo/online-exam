import { describe, expect, it } from 'vitest';
import { loadAppConfig } from '../config/loadConfig.js';

describe('loadAppConfig', () => {
  it('按 yaml -> dev yaml -> properties -> env 的顺序加载配置', () => {
    const config = loadAppConfig(process.cwd(), {
      EXAM_DB_USERNAME: 'root',
      EXAM_DB_PASSWORD: '123456',
      EXAM_NACOS_ADDR: '127.0.0.1:8848',
      EXAM_NACOS_USERNAME: 'nacos',
      EXAM_NACOS_PASSWORD: 'nacos',
      EXAM_ISSUE_NOTIFY_IP: '127.0.0.1',
      EXAM_ISSUE_NOTIFY_PORT: '8091',
      EXAM_JWT_SECRET: 'mySuperSecretKeyThatIsAtLeast32BytesLongForHS512',
      PORT: '8099'
    });

    expect(config.app.name).toBe('exam-issue-notify');
    expect(config.server.port).toBe(8099);
    expect(config.mysql.user).toBe('root');
    expect(config.nacos.servicePort).toBe(8091);
    expect(config.notify.pollIntervalMs).toBe(3000);
  });
});
