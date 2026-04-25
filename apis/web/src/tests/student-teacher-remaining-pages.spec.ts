import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import StudentClassView from '@/views/student/StudentClassView.vue';
import StudentExamView from '@/views/student/StudentExamView.vue';
import StudentScoreView from '@/views/student/StudentScoreView.vue';
import StudentIssueView from '@/views/student/StudentIssueView.vue';
import TeacherClassView from '@/views/teacher/TeacherClassView.vue';
import TeacherScoreView from '@/views/teacher/TeacherScoreView.vue';
import TeacherIssueView from '@/views/teacher/TeacherIssueView.vue';
import { prepareSession } from './test-utils';

async function settle() {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, 300));
  await flushPromises();
}

describe('学生与教师剩余专属页', () => {
  it('学生班级页会加载班级信息', { timeout: 15000 }, async () => {
    await prepareSession('student01');
    await router.push('/student/class');
    const wrapper = mount(StudentClassView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('我的班级');
    expect(wrapper.text()).toContain('Java 开发一班');
    wrapper.unmount();
  });

  it('学生考试页会加载考试列表', { timeout: 15000 }, async () => {
    await prepareSession('student01');
    await router.push('/student/exam');
    const wrapper = mount(StudentExamView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('我的考试');
    expect(wrapper.text()).toContain('Java 阶段测验');
    wrapper.unmount();
  });

  it('学生成绩页会加载成绩列表', { timeout: 15000 }, async () => {
    await prepareSession('student01');
    await router.push('/student/score');
    const wrapper = mount(StudentScoreView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('成绩查询');
    expect(wrapper.text()).toContain('86');
    wrapper.unmount();
  });

  it('学生问题页会加载问题列表', { timeout: 15000 }, async () => {
    await prepareSession('student01');
    await router.push('/student/issue');
    const wrapper = mount(StudentIssueView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('问题申报');
    expect(wrapper.text()).toContain('考试中切屏后页面卡顿');
    wrapper.unmount();
  });

  it('教师班级页会加载班级列表', { timeout: 15000 }, async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/class');
    const wrapper = mount(TeacherClassView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('班级管理');
    expect(wrapper.text()).toContain('JAVA101');
    wrapper.unmount();
  });

  it('教师成绩页会加载成绩列表', { timeout: 15000 }, async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/score');
    const wrapper = mount(TeacherScoreView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('成绩管理');
    expect(wrapper.text()).toContain('林若溪');
    wrapper.unmount();
  });

  it('教师问题页会加载问题列表', { timeout: 15000 }, async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/issue');
    const wrapper = mount(TeacherIssueView, { global: { plugins: [pinia, router, ElementPlus] } });
    await settle();
    expect(wrapper.text()).toContain('问题处理');
    expect(wrapper.text()).toContain('考试中切屏后页面卡顿');
    wrapper.unmount();
  });
});
