import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { emitMockExamConnectionEvent } from '@/api/socket';
import StudentExamAnswerView from '@/views/student/StudentExamAnswerView.vue';
import { pinia } from '@/stores';
import { router } from '@/router';
import { prepareSession } from './test-utils';

describe('学生答题页', () => {
  it('会加载考试会话与题目内容', async () => {
    await prepareSession('student01');
    await router.push('/student/exam/answer?examId=e-1');

    const wrapper = mount(StudentExamAnswerView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await flushPromises();
    await flushPromises();
    await new Promise((resolve) => setTimeout(resolve, 300));
    await flushPromises();

    expect(wrapper.text()).toContain('Java 阶段测验');
    expect(wrapper.text()).toContain('Java 中接口与抽象类的主要区别是什么');
    expect(wrapper.text()).toContain('切屏次数');
    expect(wrapper.text()).toContain('实时连接正常');

    emitMockExamConnectionEvent('disconnect', {
      examId: 'e-1',
      reason: 'network-lost'
    });

    await flushPromises();
    expect(wrapper.text()).toContain('实时连接已断开，正在尝试恢复');

    emitMockExamConnectionEvent('reconnect', {
      examId: 'e-1'
    });

    await flushPromises();
    await flushPromises();
    expect(wrapper.text()).toContain('实时连接正常');

    wrapper.unmount();
  });
});
