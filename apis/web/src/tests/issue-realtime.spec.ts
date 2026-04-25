import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import { emitMockIssueConnectionEvent, emitMockIssueEvent } from '@/api/socket';
import StudentIssueView from '@/views/student/StudentIssueView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 300));
  await flushPromises();
}

describe('问题实时通知', () => {
  it('学生问题页会响应处理进度通知', async () => {
    await prepareSession('student01');
    await router.push('/student/issue');
    const wrapper = mount(StudentIssueView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();
    expect(wrapper.text()).toContain('实时通知已连接');

    emitMockIssueConnectionEvent('disconnect', {
      scope: 'issue',
      reason: 'network-lost'
    });

    await settle();
    expect(wrapper.text()).toContain('实时通知已断开，正在尝试重连');

    emitMockIssueConnectionEvent('reconnect', {
      scope: 'issue'
    });

    await settle();
    expect(wrapper.text()).toContain('实时通知已连接');

    emitMockIssueEvent('processNotify', {
      issueId: 'i-1',
      title: '考试中切屏后页面卡顿',
      processDesc: '教师已更新处理进度',
      action: 'handled'
    });

    await settle();
    expect(wrapper.text()).toContain('已接单：考试中切屏后页面卡顿，教师已更新处理进度');
    wrapper.unmount();
  });
});
