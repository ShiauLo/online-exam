import { z, ZodError } from 'zod';
import type { FastifyInstance } from 'fastify';
import { createAppError, ErrorCatalog } from '../errors.js';
import type { AbnormalService, ProgressService, SessionService, SubmitService } from '../types.js';

const sessionSchema = z.object({
  examId: z.coerce.number().int().positive()
});

const saveProgressSchema = z.object({
  examId: z.coerce.number().int().positive(),
  answers: z.record(z.string(), z.string()).default({}),
  currentQId: z.string().optional()
});

const submitSchema = z.object({
  examId: z.coerce.number().int().positive(),
  answers: z.record(z.string(), z.string()).default({})
});

const reportAbnormalSchema = z.object({
  examId: z.coerce.number().int().positive(),
  type: z.string().min(1),
  desc: z.string().min(1).max(500),
  imgUrls: z.array(z.string()).optional(),
  screenOutCount: z.coerce.number().int().min(0).optional()
});

interface RouteDependencies {
  sessionService: SessionService;
  progressService: ProgressService;
  submitService: SubmitService;
  abnormalService: AbnormalService;
}

export async function registerRealtimeRoutes(app: FastifyInstance, deps: RouteDependencies) {
  app.post('/api/exam/realtime/session', async (request) => {
    try {
      return deps.sessionService.loadSession(sessionSchema.parse(request.body), request.requestContext);
    } catch (error) {
      if (error instanceof ZodError) {
        throw createAppError(ErrorCatalog.BAD_REQUEST, '参数错误', error.issues.map((item) => item.message));
      }
      throw error;
    }
  });

  app.post('/api/exam/realtime/save-progress', async (request) => {
    try {
      return deps.progressService.saveProgress(saveProgressSchema.parse(request.body), request.requestContext);
    } catch (error) {
      if (error instanceof ZodError) {
        throw createAppError(ErrorCatalog.BAD_REQUEST, '参数错误', error.issues.map((item) => item.message));
      }
      throw error;
    }
  });

  app.put('/api/exam/realtime/submit', async (request) => {
    try {
      return deps.submitService.submit(submitSchema.parse(request.body), request.requestContext);
    } catch (error) {
      if (error instanceof ZodError) {
        throw createAppError(ErrorCatalog.BAD_REQUEST, '参数错误', error.issues.map((item) => item.message));
      }
      throw error;
    }
  });

  app.post('/api/exam/realtime/report-abnormal', async (request) => {
    try {
      return deps.abnormalService.report(
        reportAbnormalSchema.parse(request.body),
        request.requestContext
      );
    } catch (error) {
      if (error instanceof ZodError) {
        throw createAppError(ErrorCatalog.BAD_REQUEST, '参数错误', error.issues.map((item) => item.message));
      }
      throw error;
    }
  });
}
