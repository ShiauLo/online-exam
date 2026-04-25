<template>
  <div class="error-page glass-card">
    <span class="soft-tag soft-tag--danger">{{ code }}</span>
    <h1>{{ title }}</h1>
    <p>{{ desc }}</p>
    <el-button type="primary" @click="router.push('/login')">返回入口</el-button>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

defineProps<{
  code: string;
  title: string;
  desc: string;
}>();

const router = useRouter();
</script>

<style scoped>
.error-page {
  width: min(640px, 100%);
  margin: 120px auto;
  padding: 36px;
  text-align: center;
}

.error-page h1 {
  margin: 18px 0 12px;
  font-size: 40px;
}

.error-page p {
  margin: 0 0 24px;
  line-height: 1.8;
  color: var(--text-secondary);
}
</style>
