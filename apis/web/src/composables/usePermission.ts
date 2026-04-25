import { storeToRefs } from 'pinia';
import { usePermissionStore } from '@/stores/permission';

export function usePermission() {
  const permissionStore = usePermissionStore();
  const { menus, buttons } = storeToRefs(permissionStore);

  return {
    menus,
    buttons,
    hasButton: (permission: string) => permissionStore.hasButtonPermission(permission)
  };
}
