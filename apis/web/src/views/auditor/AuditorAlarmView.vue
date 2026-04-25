<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">审计员专属页</span>
        <h1 class="page-hero__title">异常行为监控</h1>
        <p class="page-hero__desc">审计员只读查看告警级别、阈值和触发时间，不参与处置。</p>
      </div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadAlarms">刷新数据</el-button>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="告警列表" description="覆盖考试异常与系统阈值告警。" />
        <el-table :data="alarms" v-loading="loading" stripe empty-text="暂无告警">
          <el-table-column label="告警类型" min-width="150"><template #default="{ row }">{{ row.alarmType }}</template></el-table-column>
          <el-table-column label="级别" width="100"><template #default="{ row }"><el-tag :type="row.level === 'high' ? 'danger' : row.level === 'medium' ? 'warning' : 'info'" round effect="light">{{ row.level }}</el-tag></template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'resolved' ? 'success' : 'warning'" round effect="light">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="阈值" width="120"><template #default="{ row }">{{ row.threshold }}</template></el-table-column>
          <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ row.createTime }}</template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="监控说明" description="审计员职责是复核，不直接修改告警状态。" />
        <InfoPreviewCard v-if="alarms.length" badge="只读监控" badge-class="soft-tag--warning" :title="alarms[0].alarmType" :lines="alarmPreviewLines" />
        <StatePanel v-else type="empty" title="暂无监控说明" description="当前暂无告警记录，后续触发后会展示监控说明。" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { queryAlarms } from '@/api/system';
type AlarmRow = Record<string, any>;
const loading = ref(false);
const alarms = ref<AlarmRow[]>([]);
const metrics = computed(() => [
  { label: '告警总数', value: alarms.value.length },
  { label: '高等级', value: alarms.value.filter((item) => item.level === 'high').length },
  { label: '未关闭', value: alarms.value.filter((item) => item.status !== 'resolved').length }
]);
const alarmPreviewLines = computed(() => {
  const current = alarms.value[0];
  if (!current) {
    return [];
  }
  return [
    `当前级别：${current.level}`,
    `触发阈值：${current.threshold}`,
    `通知对象：${(current.recipients ?? []).join('、') || '未配置'}`
  ];
});
async function loadAlarms() {
  loading.value = true;
  try {
    const result = await queryAlarms({ pageNum: 1, pageSize: 20 });
    alarms.value = result.list;
  } finally {
    loading.value = false;
  }
}
onMounted(loadAlarms);
</script>
