import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import { usePermissionStore } from '@/stores/permission';
import TeacherClassView from '@/views/teacher/TeacherClassView.vue';
import SuperAdminDataSecurityView from '@/views/super-admin/SuperAdminDataSecurityView.vue';
import OpsDataSecurityView from '@/views/ops/OpsDataSecurityView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 300));
  await flushPromises();
}

function extractButtonTexts(wrapper: ReturnType<typeof mount>) {
  return wrapper
    .findAll('button')
    .map((item) => item.text().trim())
    .filter(Boolean);
}

describe('体验层权限显隐', () => {
  it('教师班级页会根据按钮权限隐藏操作入口', async () => {
    await prepareSession('teacher01');
    const permissionStore = usePermissionStore(pinia);
    permissionStore.payload = {
      ...permissionStore.payload,
      buttonList: []
    };

    await router.push('/teacher/class');
    const wrapper = mount(TeacherClassView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    const buttonTexts = extractButtonTexts(wrapper);
    expect(buttonTexts).not.toContain('创建班级');
    expect(buttonTexts).not.toContain('审批入班');
    expect(buttonTexts).not.toContain('移除学生');
    wrapper.unmount();
  });

  it('超级管理员数据安全页默认展示备份与恢复入口', async () => {
    await prepareSession('super01');
    await router.push('/super-admin/data-security');

    const wrapper = mount(SuperAdminDataSecurityView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    const buttonTexts = extractButtonTexts(wrapper);
    expect(buttonTexts).toContain('发起备份');
    expect(buttonTexts).toContain('恢复数据');
    wrapper.unmount();
  });

  it('运维数据安全页只展示备份入口，不展示恢复入口', async () => {
    await prepareSession('ops01');
    await router.push('/ops/data-security');

    const wrapper = mount(OpsDataSecurityView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    const buttonTexts = extractButtonTexts(wrapper);
    expect(buttonTexts).toContain('发起备份');
    expect(buttonTexts).not.toContain('恢复数据');
    wrapper.unmount();
  });
});
