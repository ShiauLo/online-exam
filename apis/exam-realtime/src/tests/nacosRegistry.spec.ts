import { afterEach, describe, expect, it, vi } from 'vitest';
import { NacosRegistry } from '../clients/nacosRegistry.js';

describe('NacosRegistry', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('会注册、心跳并注销 exam-realtime 实例', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true
    });
    vi.stubGlobal('fetch', fetchMock);

    const registry = new NacosRegistry({
      enabled: true,
      serverAddr: '127.0.0.1:8848',
      username: 'nacos',
      password: 'pwd',
      namespace: 'public',
      group: 'DEFAULT_GROUP',
      serviceIp: '127.0.0.1',
      servicePort: 8090,
      heartbeatIntervalMs: 5000
    });

    await registry.register();
    await registry.heartbeat();
    await registry.deregister();

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[0]?.[0]).toContain('/nacos/v1/ns/instance');
    expect(fetchMock.mock.calls[1]?.[0]).toContain('/nacos/v1/ns/instance/beat');
    const registerBody = String((fetchMock.mock.calls[0]?.[1] as RequestInit)?.body ?? '');
    const beatBody = String((fetchMock.mock.calls[1]?.[1] as RequestInit)?.body ?? '');
    expect(registerBody).toContain('weight=1');
    expect(registerBody).toContain('healthy=true');
    expect(registerBody).toContain('enabled=true');
    expect(beatBody).toContain('%22weight%22%3A1');
  });
});
