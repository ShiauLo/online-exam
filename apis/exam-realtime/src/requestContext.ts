import { randomUUID } from 'node:crypto';
import type { FastifyRequest } from 'fastify';
import type { RequestContext } from './types.js';
import { TokenResolver } from './auth.js';

function parseNumber(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function extractToken(authorization?: string) {
  if (!authorization) {
    return undefined;
  }
  if (authorization.startsWith('Bearer ')) {
    return authorization.slice('Bearer '.length).trim();
  }
  return authorization.trim();
}

export function resolveRequestContext(
  request: Pick<FastifyRequest, 'headers'>,
  tokenResolver: TokenResolver
): RequestContext {
  const requestId = String(request.headers['x-request-id'] ?? request.headers['request-id'] ?? randomUUID());
  const authorization = typeof request.headers.authorization === 'string' ? request.headers.authorization : undefined;
  const userId = parseNumber(request.headers['x-user-id']);
  const roleId = parseNumber(request.headers['x-role-id']);

  if (userId !== null) {
    return {
      userId,
      roleId,
      requestId,
      authorization
    };
  }

  const token = extractToken(authorization);
  if (!token) {
    return {
      userId: null,
      roleId: null,
      requestId,
      authorization
    };
  }

  return tokenResolver.resolveContextFromToken(token, requestId);
}
