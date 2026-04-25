import { setActivePinia } from 'pinia';
import { pinia } from '@/stores';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';
import { useExamStore } from '@/stores/exam';
import { router } from '@/router';

export async function prepareSession(account: string) {
  setActivePinia(pinia);
  localStorage.clear();

  const authStore = useAuthStore(pinia);
  const permissionStore = usePermissionStore(pinia);
  const examStore = useExamStore(pinia);

  authStore.clearSession();
  permissionStore.reset();
  examStore.reset();

  await router.push('/login').catch(() => undefined);
  await authStore.login({
    account,
    password: 'Exam@123'
  });
  await permissionStore.initialize(true);

  return {
    authStore,
    permissionStore,
    examStore
  };
}
