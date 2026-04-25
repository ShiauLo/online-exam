import { randomUUID } from 'node:crypto';
import type { Server as HttpServer } from 'node:http';
import { Server } from 'socket.io';
import { z } from 'zod';
import { createAppError, ErrorCatalog } from '../errors.js';
import { TokenResolver } from '../auth.js';
import type { AppConfig, ProgressService, RequestContext } from '../types.js';
import { DefaultSessionService } from '../services/sessionService.js';
import { DefaultSubmitService } from '../services/submitService.js';
import { RedisRealtimeRepository } from '../repositories/redisRealtimeRepository.js';

const enterExamSchema = z.object({
  examId: z.coerce.number().int().positive()
});

const reportScreenSchema = z.object({
  examId: z.coerce.number().int().positive(),
  screenOutCount: z.coerce.number().int().min(0)
});

interface SocketDependencies {
  config: AppConfig;
  tokenResolver: TokenResolver;
  sessionService: DefaultSessionService;
  progressService: ProgressService;
  submitService: DefaultSubmitService;
  redisRepository: RedisRealtimeRepository;
}

export class ExamRealtimeSocket {
  private readonly io: Server;
  private readonly countdownTimers = new Map<string, NodeJS.Timeout>();

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
    for (const timer of this.countdownTimers.values()) {
      clearInterval(timer);
    }
    this.countdownTimers.clear();
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
      socket.on('enterExam', async (payload) => {
        try {
          const request = enterExamSchema.parse(payload);
          const context = socket.data.context as RequestContext;
          if (context.userId === null || context.roleId !== 4) {
            throw createAppError(ErrorCatalog.REALTIME_SOCKET_FORBIDDEN);
          }

          const activeSocketId = await this.deps.redisRepository.getActiveSocketId(
            request.examId,
            context.userId
          );
          if (activeSocketId && activeSocketId !== socket.id) {
            this.io.sockets.sockets.get(activeSocketId)?.disconnect(true);
          }

          const session = await this.deps.sessionService.loadSession(
            { examId: request.examId },
            context
          );
          const roomName = `exam:${request.examId}:student:${context.userId}`;
          await socket.join(roomName);
          await this.deps.redisRepository.setActiveSocketId(
            request.examId,
            context.userId,
            socket.id,
            Math.max(session.exam.remainSeconds + this.deps.config.realtime.ttlHoursAfterExam * 3600, 60)
          );

          socket.emit('connected', {
            examId: request.examId,
            studentId: context.userId
          });

          this.startCountdown(socket.id, request.examId, context, session.exam.remainSeconds);
        } catch (error) {
          socket.emit('connect_error', {
            message: error instanceof Error ? error.message : '实时连接建立失败'
          });
        }
      });

      socket.on('reportScreen', async (payload) => {
        try {
          const request = reportScreenSchema.parse(payload);
          const context = socket.data.context as RequestContext;
          const nextCount = await this.deps.progressService.syncScreenOutCount(
            request.examId,
            request.screenOutCount,
            context
          );
          socket.emit('processNotify', {
            issueId: `screen-${request.examId}-${context.userId}`,
            status: 'warning',
            processDesc: `检测到切屏 ${nextCount} 次`
          });
        } catch (error) {
          socket.emit('connect_error', {
            message: error instanceof Error ? error.message : '切屏上报失败'
          });
        }
      });

      socket.on('disconnect', async () => {
        this.clearCountdown(socket.id);
        const context = socket.data.context as RequestContext | undefined;
        const currentExamId = Number(socket.data.examId ?? 0);
        if (context?.userId && currentExamId) {
          const activeSocketId = await this.deps.redisRepository.getActiveSocketId(currentExamId, context.userId);
          if (activeSocketId === socket.id) {
            await this.deps.redisRepository.clearActiveSocketId(currentExamId, context.userId);
          }
        }
      });
    });
  }

  private startCountdown(socketId: string, examId: number, context: RequestContext, remainSeconds: number) {
    this.clearCountdown(socketId);
    const socket = this.io.sockets.sockets.get(socketId);
    if (!socket) {
      return;
    }
    socket.data.examId = examId;
    let remaining = remainSeconds;
    socket.emit('countdown', {
      examId,
      remainTime: remaining
    });
    const timer = setInterval(async () => {
      remaining = Math.max(remaining - 1, 0);
      socket.emit('countdown', {
        examId,
        remainTime: remaining
      });
      if (remaining <= 0) {
        this.clearCountdown(socketId);
        await this.deps.submitService.autoSubmit(examId, context).catch(() => undefined);
      }
    }, this.deps.config.realtime.countdownIntervalMs);
    this.countdownTimers.set(socketId, timer);
  }

  private clearCountdown(socketId: string) {
    const timer = this.countdownTimers.get(socketId);
    if (timer) {
      clearInterval(timer);
      this.countdownTimers.delete(socketId);
    }
  }
}
