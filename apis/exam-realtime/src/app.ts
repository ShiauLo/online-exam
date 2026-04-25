import fastify, { type FastifyRequest } from 'fastify';
import cors from '@fastify/cors';
import { resolveRequestContext } from './requestContext.js';
import { TokenResolver } from './auth.js';
import type { AppConfig, RequestContext } from './types.js';
import { AppError } from './errors.js';
import { registerRealtimeRoutes } from './routes/realtimeRoutes.js';
import { registerIssueNotifyRoutes } from './routes/issueNotifyRoutes.js';

declare module 'fastify' {
  interface FastifyRequest {
    requestContext: RequestContext;
  }
}

interface AppDependencies {
  config: AppConfig;
  tokenResolver: TokenResolver;
  services: Parameters<typeof registerRealtimeRoutes>[1] & {
    issueNotifyService?: Parameters<typeof registerIssueNotifyRoutes>[1]['issueNotifyService'];
  };
}

export function createApp({ config, tokenResolver, services }: AppDependencies) {
  const app = fastify();
  const requestContextKey = Symbol('requestContext');

  app.decorateRequest('requestContext', {
    getter(this: FastifyRequest) {
      return (this as unknown as Record<PropertyKey, unknown>)[requestContextKey] as RequestContext;
    },
    setter(this: FastifyRequest, value: RequestContext) {
      (this as unknown as Record<PropertyKey, unknown>)[requestContextKey] = value;
    }
  });

  app.register(cors, {
    origin: config.server.corsOrigins === '*' ? true : config.server.corsOrigins
  });

  app.addHook('preHandler', async (request) => {
    request.requestContext = resolveRequestContext(request, tokenResolver);
  });

  app.setErrorHandler((error, request, reply) => {
    const requestId = request.requestContext?.requestId ?? 'unknown';
    if (error instanceof AppError) {
      reply.status(error.httpStatus).send({
        code: error.domainCode,
        msg: error.message,
        requestId,
        timestamp: Date.now(),
        ...(error.errors?.length ? { errors: error.errors } : {})
      });
      return;
    }

    reply.status(500).send({
      code: 500,
        msg: error instanceof Error ? error.message || '服务器内部错误' : '服务器内部错误',
      requestId,
      timestamp: Date.now()
    });
  });

  app.addHook('onSend', async (request, reply, payload) => {
    if (typeof payload !== 'string') {
      return payload;
    }

    const contentType = reply.getHeader('content-type');
    if (typeof contentType === 'string' && !contentType.includes('application/json')) {
      return payload;
    }

    try {
      const parsed = JSON.parse(payload);
      if (parsed && parsed.code !== undefined && parsed.requestId !== undefined) {
        return payload;
      }
    } catch (_error) {
      return payload;
    }

    return JSON.stringify({
      code: 200,
      msg: 'success',
      data: JSON.parse(payload),
      requestId: request.requestContext.requestId,
      timestamp: Date.now()
    });
  });

  app.register(async (instance) => {
    await registerRealtimeRoutes(instance, services);
    if (services.issueNotifyService) {
      registerIssueNotifyRoutes(instance, {
        issueNotifyService: services.issueNotifyService
      });
    }
  });

  return app;
}
