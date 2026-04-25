<template>
  <section class="metric-grid">
    <article v-for="item in items" :key="item.label" class="metric-card glass-card">
      <p class="metric-card__label">{{ item.label }}</p>
      <p class="metric-card__value">{{ item.value }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string;
    value: string | number;
  }>;
}>();
</script>
