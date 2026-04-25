import { defineStore } from 'pinia';
import { queryPermissions } from '@/api/system';
import type { PermissionPayload } from '@/types/permission';
import { featureRouteRegistry, getLandingPath, getRoutesByRole, toRouteRecord } from '@/router/route-registry';
import type { RoleType } from '@/types/auth';
import { router } from '@/router';
import { useAuthStore } from './auth';

const EMPTY_PERMISSION: PermissionPayload = {
  roleType: 'student',
  menuList: [],
  routeList: [],
  buttonList: []
};

const featureRecords = featureRouteRegistry.map((item) => toRouteRecord(item));

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    payload: EMPTY_PERMISSION,
    initialized: false,
    registeredPaths: [] as string[]
  }),
  getters: {
    menus: (state) => state.payload.menuList,
    buttons: (state) => state.payload.buttonList,
    landingPath: (state) => getLandingPath(state.payload.roleType),
    hasButtonPermission: (state) => (permission: string) =>
      state.payload.buttonList.includes(permission),
    canAccessPath: (state) => (path: string) =>
      state.payload.routeList.some((item) => item.path === path)
  },
  actions: {
    reset() {
      this.payload = EMPTY_PERMISSION;
      this.initialized = false;
      this.registeredPaths = [];
    },
    ensureRoutesForRole(role: RoleType) {
      this.registerFeatureRoutesByPaths(getRoutesByRole(role).map((item) => item.path));
    },
    async initialize(force = false) {
      const authStore = useAuthStore();
      if (!authStore.user) {
        this.reset();
        return;
      }
      this.ensureRoutesForRole(authStore.user.roleType);
      if (this.initialized && !force) {
        return;
      }
      this.payload = await queryPermissions({ accountId: authStore.user.accountId });
      this.initialized = true;
      this.registerFeatureRoutes();
    },
    registerFeatureRoutes() {
      this.registerFeatureRoutesByPaths(this.payload.routeList.map((item) => item.path));
    },
    registerFeatureRoutesByPaths(paths: string[]) {
      paths.forEach((path) => {
        if (this.registeredPaths.includes(path)) {
          return;
        }
        const matched = featureRecords.find((item) => item.path === path.replace(/^\//, ''));
        const fallback = featureRecords.find((item) => `/${item.path}` === path);
        const target = matched ?? fallback;
        if (!target) {
          return;
        }
        const resolvedPath = `/${target.path}`;
        const exists = router.getRoutes().some((item) => item.path === resolvedPath);
        if (!exists) {
          router.addRoute('dashboard', target);
        }
        this.registeredPaths.push(path);
      });
    }
  }
});
