<template>
  <div class="page-toolbar">
    <div v-if="$slots.filters" class="page-toolbar__filters">
      <slot name="filters" />
    </div>
    <div v-if="$slots.actions" class="page-toolbar__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.page-toolbar__filters,
.page-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.page-toolbar__filters {
  flex: 1 1 280px;
  justify-content: flex-end;
}

.page-toolbar__actions {
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-toolbar {
    width: 100%;
    justify-content: flex-start;
  }

  .page-toolbar__filters,
  .page-toolbar__actions {
    flex: 1 1 100%;
    justify-content: flex-start;
  }
}
</style>
