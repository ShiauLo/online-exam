<template>
  <div class="auth-shell">
    <section class="auth-panel glass-card">
      <span class="soft-tag soft-tag--warning">找回密码指引</span>
      <h1>联系管理员重置密码</h1>
      <p>
        根据接口文档，本期系统没有匿名找回密码接口；
        `/api/account/reset-password` 仅供管理员侧重置密码使用。
      </p>
      <el-alert
        title="建议处理流程"
        type="warning"
        :closable="false"
        description="请联系普通管理员或超级管理员完成密码重置，随后系统将强制你在下次登录时修改密码。"
      />
      <div style="margin-top: 20px">
        <el-button type="primary" @click="router.push('/login')">返回登录</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();
</script>

<style scoped>
.auth-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-panel {
  width: min(620px, 100%);
  padding: 28px;
}

.auth-panel h1 {
  margin: 16px 0;
  font-size: 34px;
}

.auth-panel p {
  margin: 0 0 20px;
  line-height: 1.8;
  color: var(--text-secondary);
}
</style>
