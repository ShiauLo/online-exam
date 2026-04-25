<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">审计员专属页</span>
        <h1 class="page-hero__title">日志审计</h1>
        <p class="page-hero__desc">审计员全程只读，可查看审计日志并执行导出归档。</p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="操作人 / 动作" @keyup.enter="loadLogs" />
        </template>
        <template #actions>
          <el-button @click="loadLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="system.log.export" type="success" @click="exportCurrent">导出日志</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="审计日志" description="当前仅展示审计类日志，保留只读操作边界。">
          <template #extra>
            <span class="soft-tag soft-tag--success">共 {{ total }} 条</span>
          </template>
        </SectionHeader>
        <StatePanel v-if="loadError" type="error" title="日志列表加载失败" :description="loadError" action-text="重新加载" @action="loadLogs" />
        <el-table v-else :data="logs" v-loading="loading" stripe empty-text="暂无日志">
          <el-table-column label="操作人" width="120"><template #default="{ row }">{{ row.operator }}</template></el-table-column>
          <el-table-column label="动作" min-width="180"><template #default="{ row }">{{ row.action }}</template></el-table-column>
          <el-table-column label="结果" width="110"><template #default="{ row }"><el-tag :type="row.result === 'success' ? 'success' : 'warning'" round effect="light">{{ row.result }}</el-tag></template></el-table-column>
          <el-table-column label="请求号" min-width="150"><template #default="{ row }">{{ row.requestId }}</template></el-table-column>
          <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ row.time }}</template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="审计说明" description="审计角色只查看，不参与业务改写。" />
        <InfoPreviewCard v-if="logs.length" badge="只读模式" badge-class="soft-tag--warning" :title="logs[0].operator" :lines="logPreviewLines" />
        <StatePanel v-else type="empty" title="暂无审计日志" description="当前暂无审计日志可供查看。" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryLogs, exportLogs } from '@/api/system';
import { downloadFile } from '@/api/resource';

type LogRow = Record<string, any>;
const keyword = ref('');
const total = ref(0);
const logs = ref<LogRow[]>([]);
const { confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();

const metrics = computed(() => [
  { label: '日志总数', value: total.value },
  { label: '成功记录', value: logs.value.filter((item) => item.result === 'success').length },
  { label: '待处理记录', value: logs.value.filter((item) => item.result !== 'success').length }
]);
const logPreviewLines = computed(() => {
  const current = logs.value[0];
  if (!current) {
    return [];
  }
  return [`最近动作：${current.action}`, `执行结果：${current.result}`, '审计建议：对导出和恢复等高危动作重点留痕。'];
});

async function loadLogs() {
  await runLoad(async () => {
    const result = await queryLogs({ pageNum: 1, pageSize: 20, logType: 'audit', keyword: keyword.value });
    logs.value = result.list;
    total.value = result.total;
  }, '审计日志加载失败，请稍后重试');
}

function resetFilters() {
  keyword.value = '';
  loadLogs();
}

async function exportCurrent() {
  const confirmed = await confirmAction('确认导出当前审计日志吗？', '日志导出确认');
  if (!confirmed) {
    return;
  }
  const result = await exportLogs({ logType: 'audit' });
  const download = await downloadFile({ fileKey: result.fileKey });
  ElMessage.success(`导出任务已生成：${download.fileKey}`);
}

onMounted(loadLogs);
</script>

<style scoped>
.toolbar-search{width:240px}@media (max-width:768px){.toolbar-search{width:100%}}
</style>
