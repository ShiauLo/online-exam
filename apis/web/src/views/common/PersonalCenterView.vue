<template>
  <div class="page-grid">
    <section class="page-hero">
      <div>
        <span class="soft-tag">个人中心</span>
        <h1 class="page-hero__title">个人信息与登录日志</h1>
        <p class="page-hero__desc">当前为前端独立演示模式，手机号、邮箱与演示密码会直接展示，便于核对 Mock 登录信息。</p>
      </div>
    </section>

    <section class="glass-card surface-section">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="账号">{{ authStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ authStore.user?.realName }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ authStore.user?.roleType }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ authStore.user?.phone ?? '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ authStore.user?.email ?? '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="演示密码">{{ authStore.user?.demoPassword ?? '未提供' }}</el-descriptions-item>
        <el-descriptions-item label="所属班级">{{ authStore.user?.className ?? '无' }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
</script>
