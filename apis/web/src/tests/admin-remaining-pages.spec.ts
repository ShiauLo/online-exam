import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import AdminClassView from '@/views/admin/AdminClassView.vue';
import AdminQuestionAuditView from '@/views/admin/AdminQuestionAuditView.vue';
import AdminSystemConfigView from '@/views/admin/AdminSystemConfigView.vue';
import AdminIssueView from '@/views/admin/AdminIssueView.vue';
import AdminLogView from '@/views/admin/AdminLogView.vue';
import SuperAdminLogView from '@/views/super-admin/SuperAdminLogView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 300));
  await flushPromises();
}

describe('管理员剩余专属页', () => {
  it('班级管理页会加载班级台账', async () => {
    await prepareSession('admin01');
    await router.push('/admin/class');
    const wrapper = mount(AdminClassView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('班级管理');
    expect(wrapper.text()).toContain('Java 开发一班');
    wrapper.unmount();
  });

  it('试题审核页会加载待审试题', async () => {
    await prepareSession('admin01');
    await router.push('/admin/question-audit');
    const wrapper = mount(AdminQuestionAuditView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('试题审核');
    expect(wrapper.text()).toContain('二叉树前序遍历的访问顺序是');
    wrapper.unmount();
  });

  it('系统配置页会加载配置与告警阈值', async () => {
    await prepareSession('admin01');
    await router.push('/admin/system-config');
    const wrapper = mount(AdminSystemConfigView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('系统配置');
    expect(wrapper.text()).toContain('security.login.maxFailure');
    expect(wrapper.text()).toContain('screen-out');
    wrapper.unmount();
  });

  it('业务问题页会加载问题列表', async () => {
    await prepareSession('admin01');
    await router.push('/admin/issue');
    const wrapper = mount(AdminIssueView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('业务问题');
    expect(wrapper.text()).toContain('教师注册审核待处理');
    wrapper.unmount();
  });

  it('业务日志页会加载业务日志', async () => {
    await prepareSession('admin01');
    await router.push('/admin/log');
    const wrapper = mount(AdminLogView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('业务日志');
    expect(wrapper.text()).toContain('审批学生入班');
    wrapper.unmount();
  });

  it('超级管理员日志页会加载全量日志', async () => {
    await prepareSession('super01');
    await router.push('/super-admin/log');
    const wrapper = mount(SuperAdminLogView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('日志审计');
    expect(wrapper.text()).toContain('执行增量备份');
    wrapper.unmount();
  });
});
