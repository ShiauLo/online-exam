import type { AppConfig } from '../types.js';

function toBaseUrl(serverAddr: string) {
  if (serverAddr.startsWith('http://') || serverAddr.startsWith('https://')) {
    return serverAddr;
  }
  return `http://${serverAddr}`;
}

export class NacosRegistry {
  private heartbeatTimer: NodeJS.Timeout | null = null;

  constructor(private readonly config: AppConfig['nacos']) {}

  async register() {
    if (!this.config.enabled) {
      return;
    }
    const params = this.buildParams();
    const response = await fetch(`${toBaseUrl(this.config.serverAddr)}/nacos/v1/ns/instance`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params.toString()
    });
    if (!response.ok) {
      throw new Error(`Nacos register failed: ${response.status}`);
    }
  }

  startHeartbeat() {
    if (!this.config.enabled || this.heartbeatTimer) {
      return;
    }
    this.heartbeatTimer = setInterval(() => {
      this.heartbeat().catch(() => undefined);
    }, this.config.heartbeatIntervalMs);
  }

  async heartbeat() {
    if (!this.config.enabled) {
      return;
    }
    const params = this.buildParams();
    const response = await fetch(`${toBaseUrl(this.config.serverAddr)}/nacos/v1/ns/instance/beat`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      body: params.toString()
    });
    if (!response.ok) {
      throw new Error(`Nacos heartbeat failed: ${response.status}`);
    }
  }

  async deregister() {
    if (!this.config.enabled) {
      return;
    }
    this.stopHeartbeat();
    const params = this.buildParams();
    const response = await fetch(
      `${toBaseUrl(this.config.serverAddr)}/nacos/v1/ns/instance?${params.toString()}`,
      {
        method: 'DELETE'
      }
    );
    if (!response.ok) {
      throw new Error(`Nacos deregister failed: ${response.status}`);
    }
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private buildParams() {
    const params = new URLSearchParams();
    params.set('serviceName', 'exam-issue-notify');
    params.set('groupName', this.config.group);
    params.set('namespaceId', this.config.namespace);
    params.set('ip', this.config.serviceIp);
    params.set('port', String(this.config.servicePort));
    params.set('weight', '1');
    params.set('healthy', 'true');
    params.set('enabled', 'true');
    params.set('ephemeral', 'true');
    params.set('username', this.config.username);
    params.set('password', this.config.password);
    params.set(
      'beat',
      JSON.stringify({
        ip: this.config.serviceIp,
        port: this.config.servicePort,
        serviceName: 'exam-issue-notify',
        weight: 1
      })
    );
    return params;
  }
}
