import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import FeaturePageView from '@/views/common/FeaturePageView.vue';
import { pinia } from '@/stores';
import { router } from '@/router';
import { prepareSession } from './test-utils';

describe('通用业务页', () => {
  it('管理员业务日志页会加载日志信息', async () => {
    await prepareSession('admin01');
    await router.push('/admin/log');

    const wrapper = mount(FeaturePageView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await flushPromises();
    await flushPromises();
    await new Promise((resolve) => setTimeout(resolve, 300));
    await flushPromises();

    expect(wrapper.text()).toContain('业务日志');
    expect(wrapper.text()).toContain('审批学生入班');
    expect(wrapper.text()).toContain('teacher01');

    wrapper.unmount();
  });
});
