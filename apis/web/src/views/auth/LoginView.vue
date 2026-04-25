<template>
  <div class="auth-shell">
    <section class="auth-panel glass-card">
      <div class="auth-panel__intro">
        <span class="soft-tag">文档纠偏后实现</span>
        <h1>在线考试系统登录</h1>
        <p>{{ introText }}</p>
      </div>

      <el-tabs v-model="loginMode" stretch>
        <el-tab-pane label="账号密码登录" name="password">
          <el-form :model="passwordForm" label-position="top" @submit.prevent="handlePasswordLogin">
            <el-form-item label="账号">
              <el-input v-model="passwordForm.account" placeholder="支持用户名 / 手机号 / 邮箱" />
            </el-form-item>

            <el-form-item label="密码">
              <el-input v-model="passwordForm.password" show-password type="password" placeholder="请输入密码" />
            </el-form-item>

            <el-form-item v-if="showVerifyCode" label="验证码">
              <el-input v-model="passwordForm.verifyCode" placeholder="请输入验证码 1234" />
            </el-form-item>

            <div class="auth-panel__actions">
              <el-button type="primary" :loading="loading" @click="handlePasswordLogin">登录</el-button>
              <el-button text @click="router.push('/register')">学生注册</el-button>
              <el-button text @click="router.push('/forget-password')">找回密码</el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="手机验证码登录" name="sms">
          <el-form :model="smsForm" label-position="top" @submit.prevent="handleSmsLogin">
            <el-form-item label="手机号">
              <el-input v-model="smsForm.phone" placeholder="请输入完整手机号" />
            </el-form-item>

            <el-form-item label="短信验证码">
              <div class="sms-row">
                <el-input v-model="smsForm.verifyCode" placeholder="请输入控制台中的验证码" />
                <el-button :loading="sendingSms" @click="handleSendSmsCode">发送验证码</el-button>
              </div>
            </el-form-item>

            <div class="auth-panel__actions">
              <el-button type="primary" :loading="loading" @click="handleSmsLogin">登录</el-button>
              <el-button text @click="router.push('/register')">学生注册</el-button>
              <el-button text @click="router.push('/forget-password')">找回密码</el-button>
            </div>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="auth-panel__accounts">
        <h3>{{ accountSectionTitle }}</h3>
        <p v-for="line in accountHints" :key="line">{{ line }}</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { sendSmsCode } from '@/api/auth';
import { USE_MOCK } from '@/config/runtime';
import { useAuthStore } from '@/stores/auth';
import { usePermissionStore } from '@/stores/permission';

const router = useRouter();
const authStore = useAuthStore();
const permissionStore = usePermissionStore();
const loginMode = ref<'password' | 'sms'>('password');
const loading = ref(false);
const sendingSms = ref(false);
const loginFailureCount = ref(0);
const showVerifyCode = ref(false);
const passwordForm = reactive({
  account: USE_MOCK ? 'student01' : 'student_wang',
  password: USE_MOCK ? 'Exam@123' : '',
  verifyCode: ''
});
const smsForm = reactive({
  phone: USE_MOCK ? '13810002001' : '',
  verifyCode: ''
});
const introText = computed(() =>
  USE_MOCK
    ? '当前支持账号密码登录与 Mock 手机验证码登录。短信验证码会输出到浏览器控制台，便于本地演示登录流程。'
    : '当前为真实联调模式，账号密码登录将直连后端账户服务；手机验证码登录需先保证联调库中的账号已绑定手机号且短信链路可用。'
);
const accountSectionTitle = computed(() => (USE_MOCK ? '演示账号' : '真实联调提示'));
const accountHints = computed(() =>
  USE_MOCK
    ? [
        '学生 `student01`、教师 `teacher01`、管理员 `admin01`、超管 `super01`、审计员 `auditor01`、运维 `ops01`',
        '统一密码：`Exam@123`',
        '演示手机号：学生 `13810002001`、教师 `13910003001`、管理员 `13710004001`、超管 `13610005001`、审计员 `13510006001`、运维 `13410007001`',
        '账号密码登录连续失败 5 次后，仍会要求输入图形验证码 `1234`。'
      ]
    : [
        '建议优先使用后端初始化账户，例如 `admin`、`manager`、`teacher_li`、`student_wang`、`auditor_chen`、`operator_zhao`。',
        '若已执行联调准备脚本或联调状态重置脚本，上述账号默认密码统一为 `123456`。',
        '真实模式下不要沿用 mock 演示密码；若短信链路因阿里云余额不足失败，当前不作为主链阻塞项。',
        '若需要手机验证码登录，请先确认目标账号已绑定手机号，并且 `/api/account/send/verifycode` 可正常发送验证码。'
      ]
);

async function finishLogin(payload: Parameters<typeof authStore.login>[0]) {
  loading.value = true;
  try {
    await authStore.login(payload);
    await permissionStore.initialize(true);
    router.replace(permissionStore.landingPath);
  } finally {
    loading.value = false;
  }
}

async function handlePasswordLogin() {
  try {
    await finishLogin({
      loginType: 'password_login',
      account: passwordForm.account,
      password: passwordForm.password,
      verifyCode: passwordForm.verifyCode
    });
  } catch (error) {
    loginFailureCount.value += 1;
    showVerifyCode.value = loginFailureCount.value >= 5;
    ElMessage.warning('登录失败，请检查账号密码或验证码');
  }
}

async function handleSendSmsCode() {
  sendingSms.value = true;
  try {
    await sendSmsCode(smsForm.phone);
    ElMessage.success(USE_MOCK ? 'Mock 短信验证码已发送，请查看浏览器控制台' : '验证码请求已提交，请以短信或后端日志结果为准');
  } finally {
    sendingSms.value = false;
  }
}

async function handleSmsLogin() {
  try {
    await finishLogin({
      loginType: 'one_key_login',
      phone: smsForm.phone,
      verifyCode: smsForm.verifyCode
    });
  } catch (error) {
    ElMessage.warning('短信验证码登录失败，请检查手机号或验证码');
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-panel {
  width: min(560px, 100%);
  padding: 28px;
}

.auth-panel__intro h1 {
  margin: 16px 0 10px;
  font-size: 34px;
}

.auth-panel__intro p {
  margin: 0 0 20px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.auth-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sms-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
}

.auth-panel__accounts {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  color: var(--text-secondary);
  line-height: 1.8;
}

@media (max-width: 768px) {
  .sms-row {
    grid-template-columns: 1fr;
  }
}
</style>
