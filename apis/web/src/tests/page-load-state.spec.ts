import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import * as issueApi from '@/api/issue';
import * as classApi from '@/api/class';
import * as systemApi from '@/api/system';
import StudentIssueView from '@/views/student/StudentIssueView.vue';
import TeacherClassView from '@/views/teacher/TeacherClassView.vue';
import AdminLogView from '@/views/admin/AdminLogView.vue';
import { prepareSession } from './test-utils';

async function settle(delay = 400) {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, delay));
  await flushPromises();
}

describe('页面统一状态', () => {
  it('问题页加载失败后会展示失败态并允许重试', async () => {
    const realQueryIssues = issueApi.queryIssues;
    vi.spyOn(issueApi, 'queryIssues')
      .mockRejectedValueOnce(new Error('测试问题列表加载失败'))
      .mockImplementation((payload) => realQueryIssues(payload));

    await prepareSession('student01');
    await router.push('/student/issue');

    const wrapper = mount(StudentIssueView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();
    expect(wrapper.text()).toContain('问题列表加载失败');
    expect(wrapper.text()).toContain('测试问题列表加载失败');
    expect(wrapper.text()).toContain('重新加载');

    const retryButton = wrapper.findAll('button').find((item) => item.text().includes('重新加载'));
    expect(retryButton).toBeTruthy();
    await retryButton!.trigger('click');
    await settle();

    expect(wrapper.text()).toContain('考试中切屏后页面卡顿');
    wrapper.unmount();
  });

  it('教师班级页加载失败后会展示失败态并允许重试', async () => {
    const realQueryClasses = classApi.queryClasses;
    vi.spyOn(classApi, 'queryClasses')
      .mockRejectedValueOnce(new Error('测试班级列表加载失败'))
      .mockImplementation((payload) => realQueryClasses(payload));

    await prepareSession('teacher01');
    await router.push('/teacher/class');

    const wrapper = mount(TeacherClassView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();
    expect(wrapper.text()).toContain('班级列表加载失败');
    expect(wrapper.text()).toContain('测试班级列表加载失败');
    expect(wrapper.text()).toContain('重新加载');

    const retryButton = wrapper.findAll('button').find((item) => item.text().includes('重新加载'));
    expect(retryButton).toBeTruthy();
    await retryButton!.trigger('click');
    await settle();

    expect(wrapper.text()).toContain('Java 开发一班');
    wrapper.unmount();
  });

  it('管理员业务日志页在无数据时会展示统一空态', async () => {
    vi.spyOn(systemApi, 'queryLogs').mockResolvedValueOnce({
      list: [],
      total: 0,
      pageNum: 1,
      pageSize: 20
    });

    await prepareSession('admin01');
    await router.push('/admin/log');

    const wrapper = mount(AdminLogView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();
    expect(wrapper.text()).toContain('暂无业务日志');
    expect(wrapper.text()).toContain('当前没有匹配的业务日志记录');
    wrapper.unmount();
  });
});
