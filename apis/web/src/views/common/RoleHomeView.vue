<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">角色首页</span>
        <h1 class="page-hero__title">{{ title }}</h1>
        <p class="page-hero__desc">
          当前首页会按登录角色展示核心信息和高优先级入口，后续业务页会在这个骨架上逐步接入真实数据和交互。
        </p>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in metrics" :key="card.label" class="metric-card glass-card">
        <p class="metric-card__label">{{ card.label }}</p>
        <p class="metric-card__value">{{ card.value }}</p>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const title = computed(() => String(route.meta.title ?? '工作台'));
const metrics = computed(() => [
  { label: '今日待处理事项', value: 6 },
  { label: '当前在线会话', value: 18 },
  { label: '异常提醒', value: 2 },
  { label: '最近请求 ID', value: 'REQ-LIVE' }
]);
</script>
