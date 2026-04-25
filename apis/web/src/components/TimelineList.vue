<template>
  <div class="timeline-list">
    <div v-for="item in items" :key="String(item.key)" class="timeline-item">
      <span v-if="item.tag" class="soft-tag" :class="item.tagClass">{{ item.tag }}</span>
      <h4>{{ item.title }}</h4>
      <p>{{ item.description }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
type TimelineItem = {
  key: string | number;
  tag?: string;
  tagClass?: string;
  title: string;
  description: string;
};

defineProps<{
  items: TimelineItem[];
}>();
</script>

<style scoped>
.timeline-list {
  display: grid;
  gap: 14px;
}

.timeline-item {
  padding: 16px;
  border-radius: var(--radius-small);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.timeline-item h4 {
  margin: 12px 0 6px;
}

.timeline-item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
