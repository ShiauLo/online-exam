<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">运维专属页</span>
        <h1 class="page-hero__title">系统告警</h1>
        <p class="page-hero__desc">按文档纠偏，当前页仅提供告警查询展示，不实现确认处理状态。</p>
      </div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadAlarms">刷新数据</el-button>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="surface-section glass-card">
      <SectionHeader title="告警列表" description="当前只读展示，等待后端补齐告警确认接口。" />
      <StatePanel v-if="loadError" type="error" title="告警列表加载失败" :description="loadError" action-text="重新加载" @action="loadAlarms" />
      <StatePanel v-else-if="!loading && !alarms.length" type="empty" title="暂无系统告警" description="当前没有可展示的系统告警，后续产生告警后会展示在这里。" />
      <el-table v-else :data="alarms" v-loading="loading" stripe empty-text="暂无告警">
        <el-table-column label="告警类型" min-width="150"><template #default="{ row }">{{ row.alarmType }}</template></el-table-column>
        <el-table-column label="级别" width="100"><template #default="{ row }"><el-tag :type="row.level === 'high' ? 'danger' : row.level === 'medium' ? 'warning' : 'info'" round effect="light">{{ row.level }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag round effect="light">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column label="阈值" width="120"><template #default="{ row }">{{ row.threshold }}</template></el-table-column>
        <el-table-column label="通知对象" min-width="160"><template #default="{ row }">{{ (row.recipients ?? []).join('、') }}</template></el-table-column>
        <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ row.createTime }}</template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryAlarms } from '@/api/system';
type AlarmRow = Record<string, any>;
const alarms = ref<AlarmRow[]>([]);
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '告警总数', value: alarms.value.length },
  { label: '处理中', value: alarms.value.filter((item) => item.status === 'processing').length },
  { label: '高等级', value: alarms.value.filter((item) => item.level === 'high').length }
]);
async function loadAlarms() {
  await runLoad(async () => {
    const result = await queryAlarms({ pageNum: 1, pageSize: 20 });
    alarms.value = result.list;
  }, '系统告警加载失败，请稍后重试');
}
onMounted(loadAlarms);
</script>
