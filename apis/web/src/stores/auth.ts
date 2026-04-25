import { defineStore } from 'pinia';
import type { LoginPayload, LoginResult } from '@/types/auth';
import { clearStoredTokens, clearStoredUser, getStoredTokens, getStoredUser, setStoredTokens, setStoredUser } from '@/utils/auth';
import { queryAccountSummary } from '@/api/account';
import { login, logout, refreshToken } from '@/api/auth';
import type { AuthTokens, RoleType, UserProfile } from '@/types/auth';

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const segments = token.split('.');
    if (segments.length < 2) {
      return null;
    }
    const normalized = segments[1].replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(normalized);
    return JSON.parse(decoded) as Record<string, unknown>;
  } catch (error) {
    console.warn('解析登录令牌失败，将使用兜底用户信息', error);
    return null;
  }
}

function normalizeRoleType(rawValue: unknown): RoleType | null {
  if (rawValue == null) {
    return null;
  }
  const value = String(rawValue).trim().toLowerCase();
  if (value === '1' || value === 'super_admin') return 'super_admin';
  if (value === '2' || value === 'admin') return 'admin';
  if (value === '3' || value === 'teacher') return 'teacher';
  if (value === '4' || value === 'student') return 'student';
  if (value === '5' || value === 'auditor') return 'auditor';
  if (value === '6' || value === 'operator' || value === 'ops') return 'ops';
  return null;
}

async function buildUserProfileFromToken(accessToken: string): Promise<UserProfile> {
  const claims = decodeJwtPayload(accessToken) ?? {};
  const accountId = String(claims.userId ?? claims.sub ?? '');
  const roleType = normalizeRoleType(claims.roleId ?? claims.roleType) ?? 'student';
  const fallbackProfile: UserProfile = {
    accountId,
    username: accountId ? `user_${accountId}` : 'current_user',
    realName: accountId ? `用户${accountId}` : '当前用户',
    roleType
  };

  const numericUserId = Number(accountId);
  if (!Number.isFinite(numericUserId) || numericUserId <= 0) {
    return fallbackProfile;
  }

  try {
    const result = await queryAccountSummary({
      userId: numericUserId,
      pageNum: 1,
      pageSize: 1
    }, accessToken);
    const summary = Array.isArray(result)
      ? result[0]
      : Array.isArray(result.list)
        ? result.list[0]
        : Array.isArray(result.records)
          ? result.records[0]
          : null;
    if (!summary) {
      return fallbackProfile;
    }
    return {
      accountId: String(summary.accountId ?? fallbackProfile.accountId),
      username: String(summary.username ?? fallbackProfile.username),
      realName: String(summary.realName ?? fallbackProfile.realName),
      roleType: normalizeRoleType(summary.roleType) ?? fallbackProfile.roleType,
      phone: summary.phone ? String(summary.phone) : undefined,
      email: summary.email ? String(summary.email) : undefined
    };
  } catch (error) {
    console.warn('查询当前用户摘要失败，将继续使用令牌中的兜底信息', error);
    return fallbackProfile;
  }
}

async function normalizeLoginResult(payload: LoginResult | (AuthTokens & { user?: UserProfile })): Promise<LoginResult> {
  const user = payload.user ?? await buildUserProfileFromToken(payload.accessToken);
  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken,
    user
  };
}

function normalizeRefreshTokens(payload: AuthTokens | string, currentRefreshToken: string): AuthTokens {
  if (typeof payload === 'string') {
    return {
      accessToken: payload,
      refreshToken: currentRefreshToken
    };
  }
  return {
    accessToken: payload.accessToken,
    refreshToken: payload.refreshToken || currentRefreshToken
  };
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    tokens: getStoredTokens(),
    user: getStoredUser(),
    bootstrapped: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.tokens?.accessToken && state.user),
    roleType: (state) => state.user?.roleType ?? null
  },
  actions: {
    hydrate() {
      if (this.bootstrapped) {
        return;
      }
      this.tokens = getStoredTokens();
      this.user = getStoredUser();
      this.bootstrapped = true;
    },
    setSession(payload: LoginResult) {
      this.tokens = {
        accessToken: payload.accessToken,
        refreshToken: payload.refreshToken
      };
      this.user = payload.user;
      setStoredTokens(this.tokens);
      setStoredUser(this.user);
    },
    async login(payload: LoginPayload) {
      const result = await login(payload);
      const normalized = await normalizeLoginResult(result);
      this.setSession(normalized);
      return normalized;
    },
    async refreshSession() {
      if (!this.tokens?.refreshToken) {
        throw new Error('缺少刷新令牌');
      }
      const tokens = normalizeRefreshTokens(
        await refreshToken(this.tokens.refreshToken),
        this.tokens.refreshToken
      );
      this.tokens = tokens;
      setStoredTokens(tokens);
    },
    async logout() {
      if (this.isAuthenticated && this.user?.accountId && this.tokens?.refreshToken) {
        await logout({
          userId: this.user.accountId,
          refreshToken: this.tokens.refreshToken
        });
      }
      this.clearSession();
    },
    clearSession() {
      this.tokens = null;
      this.user = null;
      clearStoredTokens();
      clearStoredUser();
    }
  }
});
