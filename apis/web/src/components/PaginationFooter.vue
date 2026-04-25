<template>
  <div class="table-footer">
    <el-pagination
      background
      layout="total, prev, pager, next"
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @current-change="$emit('change', $event)"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  currentPage: number;
  pageSize: number;
  total: number;
}>();

defineEmits<{
  (event: 'change', page: number): void;
}>();
</script>

<style scoped>
.table-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
