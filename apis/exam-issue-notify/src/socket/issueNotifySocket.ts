import type { Server as HttpServer } from 'node:http';
import { randomUUID } from 'node:crypto';
import { Server } from 'socket.io';
import { z } from 'zod';
import { TokenResolver } from '../auth.js';
import { createAppError, ErrorCatalog } from '../errors.js';
import { canUseIssueNotify } from '../roles.js';
import type { AppConfig, IssueNotifyService, RequestContext } from '../types.js';

const subscribeIssueSchema = z.object({
  scope: z.literal('issue').default('issue'),
  cursor: z.string().datetime().optional()
});

interface SocketDependencies {
  config: AppConfig;
  tokenResolver: TokenResolver;
  issueNotifyService: IssueNotifyService;
}

export class IssueNotifySocket {
  private readonly io: Server;
  private readonly pollTimers = new Map<string, NodeJS.Timeout>();

  constructor(server: HttpServer, private readonly deps: SocketDependencies) {
    this.io = new Server(server, {
      path: deps.config.server.socketPath,
      cors: {
        origin: deps.config.server.corsOrigins === '*' ? true : deps.config.server.corsOrigins
      }
    });
    this.install();
  }

  close() {
    for (const timer of this.pollTimers.values()) {
      clearInterval(timer);
    }
    this.pollTimers.clear();
    this.io.close();
  }

  private install() {
    this.io.use((socket, next) => {
      try {
        const token =
          typeof socket.handshake.auth.token === 'string'
            ? socket.handshake.auth.token
            : typeof socket.handshake.headers.authorization === 'string'
              ? socket.handshake.headers.authorization.replace(/^Bearer\s+/i, '').trim()
              : '';
        if (!token) {
          return next(createAppError(ErrorCatalog.AUTHENTICATION_REQUIRED));
        }
        const requestId =
          typeof socket.handshake.auth.requestId === 'string' && socket.handshake.auth.requestId
            ? socket.handshake.auth.requestId
            : randomUUID();
        socket.data.context = this.deps.tokenResolver.resolveContextFromToken(token, requestId);
        return next();
      } catch (error) {
        return next(error as Error);
      }
    });

    this.io.on('connection', (socket) => {
      socket.on('subscribeIssue', async (payload) => {
        try {
          const request = subscribeIssueSchema.parse(payload ?? {});
          const context = socket.data.context as RequestContext;
          if (!context.userId || !canUseIssueNotify(context.roleId)) {
            throw createAppError(ErrorCatalog.ISSUE_NOTIFY_SUBSCRIBE_FORBIDDEN);
          }
          const cursor = this.deps.issueNotifyService.resolveSubscriptionCursor(request.cursor);
          socket.data.cursor = cursor;
          socket.emit('connected', {
            scope: request.scope,
            userId: context.userId,
            cursor
          });
          this.startPolling(socket.id, context);
        } catch (error) {
          socket.emit('connect_error', {
            message: error instanceof Error ? error.message : '实时通知订阅失败'
          });
        }
      });

      socket.on('disconnect', () => {
        this.clearPolling(socket.id);
      });
    });
  }

  private startPolling(socketId: string, context: RequestContext) {
    this.clearPolling(socketId);
    const timer = setInterval(async () => {
      const socket = this.io.sockets.sockets.get(socketId);
      if (!socket) {
        this.clearPolling(socketId);
        return;
      }
      const cursor = typeof socket.data.cursor === 'string'
        ? socket.data.cursor
        : this.deps.issueNotifyService.resolveSubscriptionCursor();
      try {
        const response = await this.deps.issueNotifyService.pollNotifications(cursor, context);
        socket.data.cursor = response.cursor;
        for (const notification of response.notifications) {
          socket.emit(notification.eventType, notification);
        }
      } catch (error) {
        socket.emit('connect_error', {
          message: error instanceof Error ? error.message : '实时通知同步失败'
        });
      }
    }, this.deps.config.notify.pollIntervalMs);
    this.pollTimers.set(socketId, timer);
  }

  private clearPolling(socketId: string) {
    const timer = this.pollTimers.get(socketId);
    if (timer) {
      clearInterval(timer);
      this.pollTimers.delete(socketId);
    }
  }
}
