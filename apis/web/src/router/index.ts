import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { pinia } from '@/stores';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';
import DashboardLayout from '@/layouts/DashboardLayout.vue';
import { getRoutesByRole, toRouteRecord } from '@/router/route-registry';
import type { RoleType } from '@/types/auth';

const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '学生注册' }
  },
  {
    path: '/forget-password',
    name: 'forget-password',
    component: () => import('@/views/auth/ForgetPasswordView.vue'),
    meta: { title: '找回密码' }
  },
  {
    path: '/403',
    name: '403',
    component: () => import('@/views/common/ErrorPageView.vue'),
    props: {
      code: '403',
      title: '无操作权限',
      desc: '当前登录角色没有访问当前页面的权限，请返回角色首页。'
    }
  },
  {
    path: '/404',
    name: '404',
    component: () => import('@/views/common/ErrorPageView.vue'),
    props: {
      code: '404',
      title: '页面不存在',
      desc: '你访问的页面已失效或尚未创建。'
    }
  },
  {
    path: '/500',
    name: '500',
    component: () => import('@/views/common/ErrorPageView.vue'),
    props: {
      code: '500',
      title: '服务异常',
      desc: '请求已到达系统，但后端返回了异常，请稍后再试。'
    }
  },
  {
    path: '/',
    name: 'dashboard',
    component: DashboardLayout,
    meta: { requiresAuth: true },
    children: []
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
];

export const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
});

export function registerRoutesForRole(role: RoleType) {
  getRoutesByRole(role).forEach((route) => {
    const target = toRouteRecord(route);
    const resolvedPath = `/${target.path}`;
    const exists = router.getRoutes().some((item) => item.path === resolvedPath);
    if (!exists) {
      router.addRoute('dashboard', target);
    }
  });
}

const bootAuthStore = useAuthStore(pinia);
bootAuthStore.hydrate();
if (bootAuthStore.user) {
  registerRoutesForRole(bootAuthStore.user.roleType);
}

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia);
  const permissionStore = usePermissionStore(pinia);
  authStore.hydrate();

  const publicPaths = ['/login', '/register', '/forget-password', '/403', '/404', '/500'];
  const requiresAuth = !publicPaths.includes(to.path);

  if (!authStore.isAuthenticated && requiresAuth) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    };
  }

  if (authStore.isAuthenticated && authStore.user) {
    registerRoutesForRole(authStore.user.roleType);
  }

  if (authStore.isAuthenticated && !permissionStore.initialized) {
    try {
      await permissionStore.initialize();
    } catch (error) {
      if (!requiresAuth) {
        authStore.clearSession();
        permissionStore.reset();
        return true;
      }
      throw error;
    }
  }

  if (authStore.isAuthenticated && ['/login', '/register', '/forget-password'].includes(to.path)) {
    return permissionStore.landingPath;
  }

  if (authStore.isAuthenticated && to.path === '/') {
    return permissionStore.landingPath;
  }

  if (requiresAuth && authStore.isAuthenticated && !permissionStore.canAccessPath(to.path)) {
    return '/403';
  }

  return true;
});
