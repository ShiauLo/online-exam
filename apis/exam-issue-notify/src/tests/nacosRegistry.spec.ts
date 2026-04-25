import { afterEach, describe, expect, it, vi } from 'vitest';
import { NacosRegistry } from '../clients/nacosRegistry.js';

const fetchMock = vi.fn();

describe('NacosRegistry', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    fetchMock.mockReset();
  });

  it('会向 exam-issue-notify 注册实例', async () => {
    fetchMock.mockResolvedValue({
      ok: true
    });
    vi.stubGlobal('fetch', fetchMock);

    const registry = new NacosRegistry({
      enabled: true,
      serverAddr: '127.0.0.1:8848',
      username: 'nacos',
      password: 'nacos',
      namespace: 'public',
      group: 'DEFAULT_GROUP',
      serviceIp: '127.0.0.1',
      servicePort: 8091,
      heartbeatIntervalMs: 5000
    });

    await registry.register();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const requestUrl = String(fetchMock.mock.calls[0]?.[0] ?? '');
    expect(requestUrl).toContain('/nacos/v1/ns/instance');
    const body = String((fetchMock.mock.calls[0]?.[1] as RequestInit)?.body ?? '');
    expect(body).toContain('serviceName=exam-issue-notify');
    expect(body).toContain('weight=1');
    expect(body).toContain('healthy=true');
    expect(body).toContain('enabled=true');
  });
});
