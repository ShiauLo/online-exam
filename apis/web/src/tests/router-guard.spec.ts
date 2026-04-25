import { describe, expect, it, vi } from 'vitest';
import { registerRoutesForRole, router } from '@/router';
import { pinia } from '@/stores';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';
import { featureRouteRegistry } from '@/router/route-registry';
import { prepareSession } from './test-utils';

describe('路由守卫', () => {
  it('未登录访问业务页面时会跳转到登录页', async () => {
    localStorage.clear();
    useAuthStore(pinia).clearSession();
    usePermissionStore(pinia).reset();
    await router.push('/');
    expect(router.currentRoute.value.path).toBe('/login');
  });

  it('学生可以访问自己的考试页面', async () => {
    await prepareSession('student01');
    await router.push('/student/exam');
    expect(router.currentRoute.value.path).toBe('/student/exam');
  });

  it('模拟整页刷新后仍可进入已有权限的业务页而不是 404', async () => {
    await prepareSession('teacher01');

    featureRouteRegistry.forEach((route) => {
      if (router.hasRoute(route.name)) {
        router.removeRoute(route.name);
      }
    });

    usePermissionStore(pinia).reset();
    registerRoutesForRole(useAuthStore(pinia).user!.roleType);
    await router.push('/teacher/paper');

    expect(router.currentRoute.value.path).toBe('/teacher/paper');
  });

  it('公开页遇到权限初始化失败时会清理旧会话并保留登录页', async () => {
    const { authStore, permissionStore } = await prepareSession('student01');
    await router.push('/student/exam');
    permissionStore.reset();
    vi.spyOn(permissionStore, 'initialize').mockRejectedValue(new Error('permission service unavailable'));

    await router.push('/login');

    expect(router.currentRoute.value.path).toBe('/login');
    expect(authStore.isAuthenticated).toBe(false);
  });
});
