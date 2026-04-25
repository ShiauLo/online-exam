import { describe, expect, it } from 'vitest';
import { setActivePinia } from 'pinia';
import { pinia } from '@/stores';
import { queryClasses } from '@/api/class';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';
import { accessTokenMap, refreshTokenMap } from '@/mock/data';
import { prepareSession } from './test-utils';

describe('请求刷新链路', () => {
  it('访问令牌失效后会自动刷新并重试请求', async () => {
    const { authStore } = await prepareSession('student01');
    setActivePinia(pinia);

    authStore.tokens = {
      accessToken: 'expired-token',
      refreshToken: authStore.tokens!.refreshToken
    };

    const result = await queryClasses({
      pageNum: 1,
      pageSize: 10
    });

    expect(result.list.length).toBeGreaterThan(0);
    expect(useAuthStore(pinia).tokens?.accessToken).not.toBe('expired-token');
  });

  it('模拟整页刷新后仍可通过刷新令牌恢复权限初始化', async () => {
    const { authStore, permissionStore } = await prepareSession('student01');
    setActivePinia(pinia);

    Object.keys(accessTokenMap).forEach((key) => delete accessTokenMap[key]);
    Object.keys(refreshTokenMap).forEach((key) => delete refreshTokenMap[key]);
    permissionStore.reset();

    await usePermissionStore(pinia).initialize(true);

    expect(usePermissionStore(pinia).initialized).toBe(true);
    expect(usePermissionStore(pinia).payload.routeList.length).toBeGreaterThan(0);
    expect(authStore.tokens?.accessToken).toContain('access-u-student-01-');
  });
});
