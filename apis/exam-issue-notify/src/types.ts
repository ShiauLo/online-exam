export interface RequestContext {
  userId: number | null;
  roleId: number | null;
  requestId: string;
  authorization?: string;
}

export interface IssueNotifyPullRequest {
  cursor?: string;
  limit?: number;
}

export interface IssueNotificationItem {
  eventId: string;
  eventType: 'issueNotify' | 'processNotify';
  issueId: string;
  title: string;
  type: string;
  status: string;
  handlerId: number | null;
  handlerName?: string;
  reporterId: number;
  reporterName?: string;
  processDesc: string;
  action: 'created' | 'handled' | 'transferred' | 'closed';
  time: string;
}

export interface IssueNotifyPullView {
  scope: 'issue';
  cursor: string;
  notifications: IssueNotificationItem[];
}

export interface IssueCreatedRow {
  issueId: string;
  type: string;
  title: string;
  status: string;
  reporterId: number;
  reporterName?: string;
  handlerId: number | null;
  handlerName?: string;
  teacherIds: number[];
  description?: string;
  occurredAt: Date | string;
}

export interface IssueProcessRow {
  logId: string;
  issueId: string;
  type: string;
  title: string;
  status: string;
  reporterId: number;
  reporterName?: string;
  handlerId: number | null;
  handlerName?: string;
  teacherIds: number[];
  action: string;
  content?: string;
  occurredAt: Date | string;
}

export interface AppConfig {
  app: {
    name: string;
    profiles: string;
  };
  server: {
    host: string;
    port: number;
    socketPath: string;
    corsOrigins: string;
  };
  mysql: {
    host: string;
    port: number;
    database: string;
    user: string;
    password: string;
    poolSize: number;
  };
  nacos: {
    enabled: boolean;
    serverAddr: string;
    username: string;
    password: string;
    namespace: string;
    group: string;
    serviceIp: string;
    servicePort: number;
    heartbeatIntervalMs: number;
  };
  jwt: {
    secret: string;
  };
  notify: {
    defaultLimit: number;
    maxLimit: number;
    pollIntervalMs: number;
    historyLookbackMinutes: number;
  };
}

export interface IssueNotifyRepository {
  listCreatedIssuesSince(since: Date, limit: number, preferLatest?: boolean): Promise<IssueCreatedRow[]>;
  listProcessUpdatesSince(since: Date, limit: number, preferLatest?: boolean): Promise<IssueProcessRow[]>;
}

export interface IssueNotifyService {
  resolveSubscriptionCursor(cursor?: string): string;
  pullNotifications(request: IssueNotifyPullRequest, context: RequestContext): Promise<IssueNotifyPullView>;
  pollNotifications(cursor: string, context: RequestContext, limit?: number): Promise<IssueNotifyPullView>;
}
