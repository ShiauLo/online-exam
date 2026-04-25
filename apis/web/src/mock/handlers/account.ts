import type { ApiResponse } from '@/types/api';
import type { AuthTokens, LoginResult, RoleType } from '@/types/auth';
import type { PermissionPayload } from '@/types/permission';
import { buildPermissionPayload } from '../permission';
import {
  accessTokenMap,
  buildToken,
  findUserByAccount,
  getUserByToken,
  loginFailureCounter,
  mockLogs,
  mockUsers,
  resolveAccountIdFromToken,
  refreshTokenMap,
  smsCodeMap
} from '../data';
import {
  applyQueryFilter,
  containsRole,
  fail,
  paginate,
  requireCurrentUser,
  success,
  toUserProfile,
  type MockRequestContext
} from './shared';

export function handleAccountMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/account/login' && method === 'POST') {
    const loginType = String(data.loginType ?? 'password_login');
    let user = null as (typeof mockUsers)[number] | null;

    if (loginType === 'one_key_login') {
      const phone = String(data.phone ?? '');
      const verifyCode = String(data.verifyCode ?? '');
      user = mockUsers.find((item) => item.phone === phone) ?? null;

      if (!phone) {
        fail(400, '请输入手机号');
      }
      if (!user) {
        fail(404, '手机号未绑定有效账号');
      }
      if (!verifyCode) {
        fail(400, '请输入短信验证码');
      }
      if (smsCodeMap[phone] !== verifyCode) {
        fail(400, '短信验证码错误');
      }
      delete smsCodeMap[phone];
    } else {
      const account = String(data.account ?? '');
      const password = String(data.password ?? '');
      const verifyCode = data.verifyCode ? String(data.verifyCode) : '';
      const failureCount = loginFailureCounter[account] ?? 0;

      if (failureCount >= 5 && verifyCode !== '1234') {
        fail(400, '请输入正确验证码', ['连续失败 5 次后请输入验证码 1234']);
      }

      user = findUserByAccount(account) ?? null;
      if (!user || user.password !== password) {
        loginFailureCounter[account] = failureCount + 1;
        fail(400, '账号或密码错误', [`当前已失败 ${loginFailureCounter[account]} 次`]);
      }

      loginFailureCounter[account] = 0;
    }

    if (user.status === 'frozen') {
      fail(403, '账户已被冻结');
    }

    const accessToken = buildToken('access', user.accountId);
    const refreshToken = buildToken('refresh', user.accountId);
    accessTokenMap[accessToken] = user.accountId;
    refreshTokenMap[refreshToken] = user.accountId;

    return success<LoginResult>({
      accessToken,
      refreshToken,
      user: toUserProfile(user)
    });
  }

  if (url === '/api/account/send/verifycode' && method === 'POST') {
    const phone = String(data.phoneNumber ?? '');
    const user = mockUsers.find((item) => item.phone === phone);

    if (!phone) {
      fail(400, '请输入手机号');
    }
    if (!user) {
      fail(404, '手机号未绑定有效账号');
    }
    if (user.status !== 'active') {
      fail(403, '当前账号不可用，无法发送验证码');
    }

    const smsCode = '246810';
    smsCodeMap[phone] = smsCode;
    console.info(`[mock-sms] 向手机号 ${phone} 发送登录验证码：${smsCode}`);
    return success(true);
  }

  if (url === '/api/account/refresh-token' && method === 'POST') {
    const refreshToken = String(data.refreshToken ?? '');
    const accountId = refreshTokenMap[refreshToken] ?? resolveAccountIdFromToken(refreshToken, 'refresh');

    if (!accountId) {
      fail(401, '刷新令牌失效');
    }

    refreshTokenMap[refreshToken] = accountId;

    const user = mockUsers.find((item) => item.accountId === accountId);
    if (!user) {
      fail(401, '用户不存在');
    }

    const accessToken = buildToken('access', user.accountId);
    accessTokenMap[accessToken] = user.accountId;
    return success<AuthTokens>({ accessToken, refreshToken });
  }

  if (url === '/api/account/logout' && method === 'POST') {
    if (token) {
      delete accessTokenMap[token];
    }
    return success(true);
  }

  if (url === '/api/account/create' && method === 'POST') {
    const roleType = String(data.roleType ?? 'student') as RoleType;
    const username = String(data.username ?? '');
    const password = String(data.password ?? '');
    const actingUser = token ? getUserByToken(token) : undefined;

    if (!username || !password) {
      fail(400, '注册信息不完整');
    }

    if (mockUsers.some((item) => item.username === username)) {
      fail(409, '用户名已存在');
    }

    if (!actingUser && roleType !== 'student') {
      fail(403, '仅支持学生自助注册');
    }

    if (actingUser && !containsRole(['admin', 'super_admin'], actingUser.roleType)) {
      fail(403, '无账户创建权限');
    }

    const newUser = {
      accountId: `u-${roleType}-${mockUsers.length + 1}`,
      username,
      realName: String(data.realName ?? username),
      roleType,
      phone: String(data.phone ?? ''),
      email: String(data.email ?? ''),
      password,
      demoPassword: password,
      status: actingUser ? 'active' : 'pending'
    } as typeof mockUsers[number];

    mockUsers.push(newUser);
    return success(toUserProfile(newUser));
  }

  if (url === '/api/system/permission/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let role = currentUser.roleType;
    const accountId = String(data.accountId ?? '');

    if (accountId && containsRole(['admin', 'super_admin'], currentUser.roleType)) {
      const targetUser = mockUsers.find((item) => item.accountId === accountId);
      if (targetUser) {
        role = targetUser.roleType;
      }
    }

    return success<PermissionPayload>(buildPermissionPayload(role));
  }

  if (url === '/api/account/update' && method === 'PUT') {
    const currentUser = requireCurrentUser(token);
    const accountId = String(data.accountId ?? currentUser.accountId);
    const user = mockUsers.find((item) => item.accountId === accountId);

    if (!user) {
      fail(404, '账户不存在');
    }

    user.realName = String(data.realName ?? user.realName);
    user.phone = String(data.phone ?? user.phone ?? '');
    user.email = String(data.email ?? user.email ?? '');
    return success(toUserProfile(user));
  }

  if (url === '/api/account/query' && method === 'POST') {
    let list = [...mockUsers];

    if (data.roleType) {
      list = list.filter((item) => item.roleType === data.roleType);
    }

    list = applyQueryFilter(list, String(data.keyword ?? ''), (item) => [
      item.username,
      item.realName,
      item.email ?? '',
      item.phone ?? ''
    ]);

    return success(
      paginate(
        list.map(toUserProfile),
        Number(data.pageNum ?? 1),
        Number(data.pageSize ?? 10)
      )
    );
  }

  if (url === '/api/account/audit' && method === 'PUT') {
    const user = mockUsers.find((item) => item.accountId === String(data.accountId ?? ''));
    if (!user) {
      fail(404, '审核对象不存在');
    }
    user.status = String(data.auditResult ?? 'reject') === 'approve' ? 'active' : 'frozen';
    return success(true);
  }

  if (url === '/api/account/freeze' && method === 'PUT') {
    const user = mockUsers.find((item) => item.accountId === String(data.accountId ?? ''));
    if (!user) {
      fail(404, '账户不存在');
    }
    user.status = Boolean(data.isFrozen) ? 'frozen' : 'active';
    return success(true);
  }

  if (url === '/api/account/reset-password' && method === 'PUT') {
    const user = mockUsers.find((item) => item.accountId === String(data.accountId ?? ''));
    if (!user) {
      fail(404, '账户不存在');
    }
    user.password = String(data.newPassword ?? '');
    return success(true);
  }

  if (url === '/api/account/login-log/query' && method === 'POST') {
    return success(
      paginate(mockLogs, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  return null;
}
