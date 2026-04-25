import { createAppError, ErrorCatalog } from '../errors.js';
import { canUseIssueNotify, isAdmin, isAuditor, isOps, isTeacher } from '../roles.js';
import type {
  AppConfig,
  IssueCreatedRow,
  IssueNotificationItem,
  IssueNotifyPullRequest,
  IssueNotifyPullView,
  IssueNotifyRepository,
  IssueNotifyService,
  IssueProcessRow,
  RequestContext
} from '../types.js';

function clampLimit(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

function toIsoString(value: Date | string) {
  const parsed = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return new Date(0).toISOString();
  }
  return parsed.toISOString();
}

function normalizeAction(action: string): IssueNotificationItem['action'] {
  const raw = action.trim().toUpperCase();
  if (raw === 'HANDLED') {
    return 'handled';
  }
  if (raw === 'TRANSFERRED') {
    return 'transferred';
  }
  if (raw === 'CLOSED') {
    return 'closed';
  }
  return 'created';
}

export class DefaultIssueNotifyService implements IssueNotifyService {
  constructor(
    private readonly repository: IssueNotifyRepository,
    private readonly config: AppConfig
  ) {}

  resolveSubscriptionCursor(cursor?: string) {
    if (!cursor) {
      return new Date().toISOString();
    }
    return this.parseCursor(cursor).toISOString();
  }

  async pullNotifications(request: IssueNotifyPullRequest, context: RequestContext): Promise<IssueNotifyPullView> {
    const cursor = request.cursor
      ? this.parseCursor(request.cursor)
      : new Date(Date.now() - this.config.notify.historyLookbackMinutes * 60 * 1000);
    return this.loadNotifications(cursor, context, request.limit, !request.cursor);
  }

  async pollNotifications(cursor: string, context: RequestContext, limit?: number): Promise<IssueNotifyPullView> {
    return this.loadNotifications(this.parseCursor(cursor), context, limit, false);
  }

  private async loadNotifications(
    since: Date,
    context: RequestContext,
    limit?: number,
    preferLatest = false
  ) {
    this.assertContext(context, ErrorCatalog.ISSUE_NOTIFY_FORBIDDEN);
    const pageSize = clampLimit(
      limit ?? this.config.notify.defaultLimit,
      1,
      this.config.notify.maxLimit
    );

    const [created, processed] = await Promise.all([
      this.repository.listCreatedIssuesSince(since, pageSize, preferLatest),
      this.repository.listProcessUpdatesSince(since, pageSize, preferLatest)
    ]);

    const sortedNotifications = [
      ...created.map((item) => this.toCreatedNotification(item)),
      ...processed.map((item) => this.toProcessNotification(item))
    ]
      .filter((item) => this.isVisible(item, context))
      .sort((left, right) => {
        if (left.time === right.time) {
          return left.eventId.localeCompare(right.eventId);
        }
        return left.time.localeCompare(right.time);
      });

    const notifications = sortedNotifications
      .slice(preferLatest ? -pageSize : 0, preferLatest ? undefined : pageSize)
      .map(({ teacherIds: _teacherIds, ...item }) => item);

    return {
      scope: 'issue' as const,
      cursor: notifications.length ? notifications[notifications.length - 1].time : since.toISOString(),
      notifications
    };
  }

  private assertContext(
    context: RequestContext,
    error: {
      httpStatus: number;
      code: number;
      message: string;
    } = ErrorCatalog.ISSUE_NOTIFY_SUBSCRIBE_FORBIDDEN
  ) {
    if (!context.userId || !canUseIssueNotify(context.roleId)) {
      throw createAppError(error);
    }
  }

  private parseCursor(cursor: string) {
    const parsed = new Date(cursor);
    if (Number.isNaN(parsed.getTime())) {
      throw createAppError(ErrorCatalog.ISSUE_NOTIFY_CURSOR_INVALID);
    }
    return parsed;
  }

  private toCreatedNotification(item: IssueCreatedRow) {
    return {
      eventId: `issue-created-${item.issueId}-${toIsoString(item.occurredAt)}`,
      eventType: 'issueNotify' as const,
      issueId: item.issueId,
      title: item.title,
      type: item.type,
      status: item.status,
      handlerId: item.handlerId ?? null,
      handlerName: item.handlerName,
      reporterId: item.reporterId,
      reporterName: item.reporterName,
      processDesc: item.description?.trim() || '收到新问题申报',
      action: 'created' as const,
      time: toIsoString(item.occurredAt),
      teacherIds: item.teacherIds
    };
  }

  private toProcessNotification(item: IssueProcessRow) {
    const action = normalizeAction(item.action);
    return {
      eventId: `issue-process-${item.logId}-${toIsoString(item.occurredAt)}`,
      eventType: 'processNotify' as const,
      issueId: item.issueId,
      title: item.title,
      type: item.type,
      status: item.status,
      handlerId: item.handlerId ?? null,
      handlerName: item.handlerName,
      reporterId: item.reporterId,
      reporterName: item.reporterName,
      processDesc: item.content?.trim() || '问题处理进度已更新',
      action,
      time: toIsoString(item.occurredAt),
      teacherIds: item.teacherIds
    };
  }

  private isVisible(
    item: IssueNotificationItem & { teacherIds: number[] },
    context: RequestContext
  ) {
    if (!context.userId || context.roleId === null) {
      return false;
    }
    if (isAuditor(context.roleId)) {
      return true;
    }
    if (item.eventType === 'issueNotify') {
      if (item.type === 'BUSINESS') {
        return isAdmin(context.roleId);
      }
      if (item.type === 'SYSTEM') {
        return isOps(context.roleId);
      }
      if (item.type === 'EXAM') {
        return isTeacher(context.roleId) && item.teacherIds.includes(context.userId);
      }
      return false;
    }

    return item.reporterId === context.userId || item.handlerId === context.userId;
  }
}
