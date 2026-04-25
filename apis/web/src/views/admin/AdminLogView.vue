<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">管理员专属页</span><h1 class="page-hero__title">业务日志</h1><p class="page-hero__desc">普通管理员仅可查看业务日志，不提供审计导出。</p></div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="操作人 / 动作" @keyup.enter="loadLogs" />
        </template>
        <template #actions>
          <el-button @click="loadLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </template>
      </PageToolbar>
    </section>
    <section class="surface-section glass-card">
      <SectionHeader title="业务日志列表" description="仅展示业务类操作日志。" />
      <StatePanel v-if="loadError" type="error" title="业务日志加载失败" :description="loadError" action-text="重新加载" @action="loadLogs" />
      <StatePanel v-else-if="!loading && !logs.length" type="empty" title="暂无业务日志" description="当前没有匹配的业务日志记录，可调整筛选条件后重试。" />
      <el-table v-else :data="logs" v-loading="loading" stripe empty-text="暂无日志">
        <el-table-column label="操作人" width="120"><template #default="{ row }">{{ row.operator }}</template></el-table-column>
        <el-table-column label="动作" min-width="180"><template #default="{ row }">{{ row.action }}</template></el-table-column>
        <el-table-column label="结果" width="100"><template #default="{ row }"><el-tag :type="row.result === 'success' ? 'success' : 'warning'" round effect="light">{{ row.result }}</el-tag></template></el-table-column>
        <el-table-column label="请求号" min-width="150"><template #default="{ row }">{{ row.requestId }}</template></el-table-column>
        <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ row.time }}</template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryLogs } from '@/api/system';
type LogRow = Record<string, any>;
const keyword = ref(''); const logs = ref<LogRow[]>([]);
const { loading, loadError, runLoad } = useLoadFeedback();
async function loadLogs() { await runLoad(async () => { const result = await queryLogs({ pageNum: 1, pageSize: 20, logType: 'business', keyword: keyword.value }); logs.value = result.list; }, '业务日志加载失败，请稍后重试'); }
function resetFilters() { keyword.value = ''; loadLogs(); }
onMounted(loadLogs);
</script>

<style scoped>
.toolbar-search{width:240px}@media (max-width:768px){.toolbar-search{width:100%}}
</style>
