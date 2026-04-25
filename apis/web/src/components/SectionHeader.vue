<template>
  <div class="section-header">
    <div class="section-header__content">
      <h3>{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.extra" class="section-header__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.section-header__content h3 {
  margin: 0 0 6px;
  font-size: 20px;
}

.section-header__content p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
  }
}
</style>
