import ElementPlus, { ElMessageBox } from 'element-plus';
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { pinia } from '@/stores';
import { router } from '@/router';
import { mockExams, mockScores } from '@/mock/data';
import TeacherClassView from '@/views/teacher/TeacherClassView.vue';
import TeacherExamView from '@/views/teacher/TeacherExamView.vue';
import TeacherScoreView from '@/views/teacher/TeacherScoreView.vue';
import SuperAdminDataSecurityView from '@/views/super-admin/SuperAdminDataSecurityView.vue';
import AuditorIssueView from '@/views/auditor/AuditorIssueView.vue';
import { prepareSession } from './test-utils';

async function settle(delay = 400) {
  await flushPromises();
  await flushPromises();
  await new Promise((resolve) => setTimeout(resolve, delay));
  await flushPromises();
}

function createWrapper(
  component: Parameters<typeof mount>[0],
  options?: {
    preserveTeleport?: boolean;
  }
) {
  return mount(component, {
    attachTo: document.body,
    global: {
      plugins: [pinia, router, ElementPlus],
      ...(options?.preserveTeleport
        ? {}
        : {
            stubs: {
              teleport: true
            }
          })
    }
  });
}

function extractButtonTexts(wrapper: VueWrapper) {
  return wrapper
    .findAll('button')
    .map((item) => item.text().trim())
    .filter(Boolean);
}

async function clickButton(wrapper: VueWrapper, label: string) {
  const button = wrapper.findAll('button').find((item) => item.text().includes(label));
  expect(button, `未找到按钮：${label}`).toBeTruthy();
  await button!.trigger('click');
}

afterEach(() => {
  vi.restoreAllMocks();
  mockExams[0].status = 'underway';
  mockExams[0].isPaused = false;
  mockScores[0].publishStatus = 'published';
});

describe('关键业务验收', () => {
  it('教师最多创建 1 个班级', async () => {
    await prepareSession('teacher01');
    await router.push('/teacher/class');
    const wrapper = createWrapper(TeacherClassView);

    await settle();

    expect(wrapper.text()).toContain('已达到 1 个班级上限');
    const createButton = wrapper.findAll('button').find((item) => item.text().includes('创建班级'));
    expect(createButton?.attributes('disabled')).toBeDefined();
    wrapper.unmount();
  });

  it('教师可以暂停考试', async () => {
    mockExams[0].status = 'underway';
    mockExams[0].isPaused = false;
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never);

    await prepareSession('teacher01');
    await router.push('/teacher/exam');
    const wrapper = createWrapper(TeacherExamView);

    await settle(700);
    await clickButton(wrapper, '暂停');
    await settle(700);

    expect(mockExams[0].status).toBe('paused');
    expect(wrapper.text()).toContain('已暂停');
    wrapper.unmount();
  });

  it('教师可以发布成绩', async () => {
    mockScores[0].publishStatus = 'draft';
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never);

    await prepareSession('teacher01');
    await router.push('/teacher/score');
    const wrapper = createWrapper(TeacherScoreView);

    await settle();
    await clickButton(wrapper, '发布');
    await settle();

    expect(mockScores[0].publishStatus).toBe('published');
    wrapper.unmount();
  });

  it('超级管理员恢复数据必须通过双验证码校验', async () => {
    const confirmSpy = vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never);

    await prepareSession('super01');
    await router.push('/super-admin/data-security');
    const wrapper = createWrapper(SuperAdminDataSecurityView, { preserveTeleport: true });

    await settle();
    await clickButton(wrapper, '恢复数据');
    await settle();

    const inputs = Array.from(document.body.querySelectorAll('.restore-form input'));
    expect(inputs.length).toBeGreaterThanOrEqual(3);

    (inputs[1] as HTMLInputElement).value = '9527';
    inputs[1].dispatchEvent(new Event('input'));
    (inputs[2] as HTMLInputElement).value = '9527';
    inputs[2].dispatchEvent(new Event('input'));
    await flushPromises();
    await clickButton(wrapper, '确认恢复');
    await settle();
    expect(confirmSpy).not.toHaveBeenCalled();

    (inputs[2] as HTMLInputElement).value = '3141';
    inputs[2].dispatchEvent(new Event('input'));
    await flushPromises();
    await clickButton(wrapper, '确认恢复');
    await settle();
    expect(confirmSpy).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it('审计员问题页保持只读，不展示处理按钮', async () => {
    await prepareSession('auditor01');
    await router.push('/auditor/issue');
    const wrapper = createWrapper(AuditorIssueView);

    await settle();

    const buttonTexts = extractButtonTexts(wrapper);
    expect(buttonTexts).toContain('刷新数据');
    expect(buttonTexts).toContain('查看进度');
    expect(buttonTexts).not.toContain('接单');
    expect(buttonTexts).not.toContain('转派');
    expect(buttonTexts).not.toContain('关闭');
    wrapper.unmount();
  });
});
