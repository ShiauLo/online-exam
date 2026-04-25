import ElementPlus from 'element-plus';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { setActivePinia } from 'pinia';
import { sendSmsCode } from '@/api/auth';
import { pinia } from '@/stores';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';
import { router } from '@/router';
import PersonalCenterView from '@/views/common/PersonalCenterView.vue';
import { prepareSession } from './test-utils';

describe('认证与个人中心演示体验', () => {
  beforeEach(() => {
    setActivePinia(pinia);
    localStorage.clear();
    useAuthStore(pinia).clearSession();
    usePermissionStore(pinia).reset();
  });

  it('Mock 短信验证码会输出到控制台并允许登录', async () => {
    const consoleSpy = vi.spyOn(console, 'info').mockImplementation(() => undefined);

    await sendSmsCode('13810002001');

    expect(consoleSpy).toHaveBeenCalledWith(
      '[mock-sms] 向手机号 13810002001 发送登录验证码：246810'
    );

    await useAuthStore(pinia).login({
      loginType: 'one_key_login',
      phone: '13810002001',
      verifyCode: '246810'
    });

    expect(useAuthStore(pinia).user?.username).toBe('student01');
    consoleSpy.mockRestore();
  });

  it('个人中心会展示手机号与演示密码', async () => {
    await prepareSession('student01');
    await router.push('/personal');

    const wrapper = mount(PersonalCenterView, {
      global: {
        plugins: [pinia, router, ElementPlus]
      }
    });

    expect(wrapper.text()).toContain('13810002001');
    expect(wrapper.text()).toContain('Exam@123');
    wrapper.unmount();
  });
});
