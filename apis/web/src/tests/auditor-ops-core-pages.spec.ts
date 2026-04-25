import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import AuditorLogView from '@/views/auditor/AuditorLogView.vue';
import AuditorScoreAuditView from '@/views/auditor/AuditorScoreAuditView.vue';
import AuditorAlarmView from '@/views/auditor/AuditorAlarmView.vue';
import OpsIssueView from '@/views/ops/OpsIssueView.vue';
import OpsDataSecurityView from '@/views/ops/OpsDataSecurityView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 300));
  await flushPromises();
}

describe('审计员与运维核心页', () => {
  it('审计日志页会加载审计日志', { timeout: 15000 }, async () => {
    await prepareSession('auditor01');
    await router.push('/auditor/log');
    const wrapper = mount(AuditorLogView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('日志审计');
    expect(wrapper.text()).toContain('导出日志审批单');
    wrapper.unmount();
  });

  it('成绩核查页会加载成绩和明细入口', { timeout: 15000 }, async () => {
    await prepareSession('auditor01');
    await router.push('/auditor/score-audit');
    const wrapper = mount(AuditorScoreAuditView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('成绩核查');
    expect(wrapper.text()).toContain('Java 阶段测验');
    expect(wrapper.text()).toContain('查看明细');
    wrapper.unmount();
  });

  it('异常行为监控页会加载告警列表', { timeout: 15000 }, async () => {
    await prepareSession('auditor01');
    await router.push('/auditor/alarm');
    const wrapper = mount(AuditorAlarmView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('异常行为监控');
    expect(wrapper.text()).toContain('screen-out');
    wrapper.unmount();
  });

  it('运维问题页会加载系统问题', { timeout: 15000 }, async () => {
    await prepareSession('ops01');
    await router.push('/ops/issue');
    const wrapper = mount(OpsIssueView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('系统类问题');
    expect(wrapper.text()).toContain('备份任务执行时间过长');
    wrapper.unmount();
  });

  it('运维数据安全页会加载备份记录', { timeout: 15000 }, async () => {
    await prepareSession('ops01');
    await router.push('/ops/data-security');
    const wrapper = mount(OpsDataSecurityView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('数据安全中心');
    expect(wrapper.text()).toContain('b-1');
    expect(wrapper.text()).toContain('禁止恢复');
    wrapper.unmount();
  });
});
