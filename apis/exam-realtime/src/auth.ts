import jwt from 'jsonwebtoken';
import type { RequestContext } from './types.js';
import { createAppError, ErrorCatalog } from './errors.js';

interface JwtClaims {
  userId?: string | number;
  roleId?: string | number;
  sub?: string;
}

export class TokenResolver {
  constructor(private readonly secret: string) {}

  resolveContextFromToken(token: string, requestId: string): RequestContext {
    try {
      const payload = jwt.verify(token, this.secret) as JwtClaims;
      const userId = Number(payload.userId ?? payload.sub);
      const roleId = payload.roleId === undefined ? null : Number(payload.roleId);
      if (!Number.isFinite(userId)) {
        throw new Error('missing userId');
      }
      return {
        userId,
        roleId: Number.isFinite(roleId) ? roleId : null,
        requestId,
        authorization: `Bearer ${token}`
      };
    } catch (error) {
      throw createAppError(ErrorCatalog.AUTHENTICATION_REQUIRED, '登录凭证无效');
    }
  }
}
