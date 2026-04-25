<template>
  <el-button v-if="visible" v-bind="$attrs">
    <slot />
  </el-button>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { usePermission } from '@/composables/usePermission';

defineOptions({
  name: 'AuthorizedAction',
  inheritAttrs: false
});

const props = withDefaults(
  defineProps<{
    permission?: string;
    when?: boolean;
  }>(),
  {
    permission: '',
    when: true
  }
);

const { hasButton } = usePermission();

const visible = computed(() => {
  if (!props.when) {
    return false;
  }

  if (!props.permission) {
    return true;
  }

  return hasButton(props.permission);
});
</script>
