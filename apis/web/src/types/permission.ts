import type { RoleType } from './auth';

export interface MenuItem {
  menuId: string;
  menuName: string;
  path: string;
  icon: string;
}

export interface PermissionRoute {
  path: string;
  component: string;
}

export type ButtonPermission = string;

export interface PermissionPayload {
  roleType: RoleType;
  menuList: MenuItem[];
  routeList: PermissionRoute[];
  buttonList: ButtonPermission[];
}
