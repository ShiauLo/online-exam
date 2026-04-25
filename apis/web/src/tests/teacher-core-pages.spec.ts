import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import TeacherQuestionView from '@/views/teacher/TeacherQuestionView.vue';
import TeacherPaperView from '@/views/teacher/TeacherPaperView.vue';
import TeacherExamView from '@/views/teacher/TeacherExamView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 700));
  await flushPromises();
}

describe('教师核心业务页', () => {
  it('试题管理页会加载分类和题目', { timeout: 15000 }, async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/question');

    const wrapper = mount(TeacherQuestionView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    expect(wrapper.text()).toContain('试题管理');
    expect(wrapper.text()).toContain('Java 基础');
    expect(wrapper.text()).toContain('HashMap 扩容时需要关注的两个风险点');
    wrapper.unmount();
  });

  it('试卷管理页会加载试卷和题目池', { timeout: 15000 }, async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/paper');

    const wrapper = mount(TeacherPaperView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    expect(wrapper.text()).toContain('试卷管理');
    expect(wrapper.text()).toContain('Java 基础摸底卷');
    expect(wrapper.text()).toContain('Java 中接口与抽象类的主要区别是什么');
    wrapper.unmount();
  });

  it('考试管理页会加载考试列表和班级信息', { timeout: 15000 }, async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/exam');

    const wrapper = mount(TeacherExamView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    await settle();

    expect(wrapper.text()).toContain('考试管理');
    expect(wrapper.text()).toContain('Java 阶段测验');
    expect(wrapper.text()).toContain('Java 开发一班');
    wrapper.unmount();
  });
});
