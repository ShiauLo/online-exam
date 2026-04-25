<template>
  <div class="form-grid" :style="{ '--form-grid-columns': resolvedColumns }">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    columns?: number;
  }>(),
  {
    columns: 2
  }
);

const resolvedColumns = computed(() => `repeat(${Math.max(1, props.columns)}, minmax(0, 1fr))`);
</script>

<style scoped>
.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: var(--form-grid-columns);
}

@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
