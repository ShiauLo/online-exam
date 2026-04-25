import type { FastifyInstance } from 'fastify';
import { z } from 'zod';
import type { IssueNotifyService } from '../types.js';
import { createAppError, ErrorCatalog } from '../errors.js';

const pullRequestSchema = z.object({
  cursor: z.string().datetime().optional(),
  limit: z.coerce.number().int().positive().max(100).optional()
});

export function registerIssueNotifyRoutes(app: FastifyInstance, services: { issueNotifyService: IssueNotifyService }) {
  app.post('/api/issue/notify', async (request) => {
    if (!request.requestContext.userId) {
      throw createAppError(ErrorCatalog.AUTHENTICATION_REQUIRED);
    }
    const payload = pullRequestSchema.parse(request.body ?? {});
    return services.issueNotifyService.pullNotifications(payload, request.requestContext);
  });
}
