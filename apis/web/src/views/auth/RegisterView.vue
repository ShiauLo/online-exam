<template>
  <div class="auth-shell">
    <section class="auth-panel glass-card">
      <div class="auth-panel__intro">
        <span class="soft-tag">学生自助注册</span>
        <h1>学生注册</h1>
        <p>按文档修正后，本页只支持学生自助注册，提交成功后需要等待管理员审核。</p>
      </div>

      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password type="password" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSubmit">提交注册</el-button>
        <el-button text @click="router.push('/login')">返回登录</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { registerStudent } from '@/api/auth';

const router = useRouter();
const loading = ref(false);
const form = reactive({
  roleType: 'student',
  username: '',
  realName: '',
  email: '',
  password: 'Exam@123'
});

async function handleSubmit() {
  loading.value = true;
  try {
    await registerStudent(form);
    ElMessage.success('注册成功，请等待管理员审核');
    router.replace('/login');
  } finally {
    loading.value = false;
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
</style>
