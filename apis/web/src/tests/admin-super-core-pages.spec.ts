import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import AdminAccountView from '@/views/admin/AdminAccountView.vue';
import SuperAdminRoleView from '@/views/super-admin/SuperAdminRoleView.vue';
import SuperAdminDataSecurityView from '@/views/super-admin/SuperAdminDataSecurityView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 300));
  await flushPromises();
}

describe('管理员与超级管理员核心页', () => {
  it('账户管理页会加载账户列表', { timeout: 15000 }, async () => {
    await prepareSession('admin01');
    await router.push('/admin/account');

    const wrapper = mount(AdminAccountView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    expect(wrapper.text()).toContain('账户管理');
    expect(wrapper.text()).toContain('student01');
    expect(wrapper.text()).toContain('周明哲');
    wrapper.unmount();
  });

  it('角色权限页会展示权限模板', { timeout: 15000 }, async () => {
    await prepareSession('super01');
    await router.push('/super-admin/role');

    const wrapper = mount(SuperAdminRoleView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    expect(wrapper.text()).toContain('角色权限管理');
    expect(wrapper.text()).toContain('教师标准权限');
    expect(wrapper.text()).toContain('question.audit');
    wrapper.unmount();
  });

  it('数据安全页会展示备份记录', { timeout: 15000 }, async () => {
    await prepareSession('super01');
    await router.push('/super-admin/data-security');

    const wrapper = mount(SuperAdminDataSecurityView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    expect(wrapper.text()).toContain('数据安全中心');
    expect(wrapper.text()).toContain('b-1');
    expect(wrapper.text()).toContain('全量备份');
    wrapper.unmount();
  });
});
