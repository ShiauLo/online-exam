<template>
  <div class="state-panel" :class="`state-panel--${type}`">
    <el-empty :image-size="96">
      <template #description>
        <div class="state-panel__body">
          <h4>{{ resolvedTitle }}</h4>
          <p>{{ description }}</p>
        </div>
      </template>
      <el-button v-if="actionText" :type="type === 'error' ? 'danger' : 'primary'" @click="$emit('action')">
        {{ actionText }}
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    type?: 'empty' | 'error';
    title?: string;
    description: string;
    actionText?: string;
  }>(),
  {
    type: 'empty',
    title: '',
    actionText: ''
  }
);

defineEmits<{
  (event: 'action'): void;
}>();

const resolvedTitle = computed(() => {
  if (props.title) {
    return props.title;
  }
  return props.type === 'error' ? '加载失败' : '暂无内容';
});
</script>

<style scoped>
.state-panel {
  min-height: 240px;
  display: grid;
  place-items: center;
  padding: 12px;
}

.state-panel__body {
  display: grid;
  gap: 8px;
  text-align: center;
}

.state-panel__body h4 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.state-panel__body p {
  margin: 0;
  line-height: 1.7;
  color: var(--text-secondary);
}
</style>
