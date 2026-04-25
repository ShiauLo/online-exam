<template>
  <div class="preview-card">
    <span v-if="badge" class="soft-tag" :class="badgeClass">{{ badge }}</span>
    <h3>{{ title }}</h3>
    <p v-for="(line, index) in lines" :key="`${title}-${index}`">{{ line }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    badge?: string;
    badgeClass?: string;
    title: string;
    lines: string[];
  }>(),
  {
    badge: '',
    badgeClass: ''
  }
);
</script>

<style scoped>
.preview-card {
  padding: 18px;
  border-radius: var(--radius-small);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.preview-card h3 {
  margin: 12px 0;
}

.preview-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
