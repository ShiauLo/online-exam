import type { PermissionPayload } from '@/types/permission';
import type { RoleType } from '@/types/auth';
import { getRoutesByRole, roleButtonPermissions } from '@/router/route-registry';

export function buildPermissionPayload(role: RoleType): PermissionPayload {
  const routes = getRoutesByRole(role);

  return {
    roleType: role,
    menuList: routes
      .filter((item) => item.meta.menuId && !item.meta.hidden)
      .map((item) => ({
        menuId: item.meta.menuId!,
        menuName: item.meta.menuName ?? item.meta.title,
        path: item.path,
        icon: item.meta.icon ?? 'Menu'
      })),
    routeList: routes.map((item) => ({
      path: item.path,
      component: item.componentKey
    })),
    buttonList: roleButtonPermissions[role]
  };
}
