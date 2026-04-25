<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">超级管理员专属页</span><h1 class="page-hero__title">日志审计</h1><p class="page-hero__desc">查看全量日志并支持导出归档，覆盖业务、系统和审计三类日志。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadLogs">刷新数据</el-button>
          <AuthorizedAction permission="system.log.export" type="success" @click="exportCurrent">导出日志</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <section class="surface-section glass-card">
      <SectionHeader title="全量日志" description="超级管理员可以查看和导出全部日志。" />
      <StatePanel v-if="loadError" type="error" title="全量日志加载失败" :description="loadError" action-text="重新加载" @action="loadLogs" />
      <StatePanel v-else-if="!loading && !logs.length" type="empty" title="暂无全量日志" description="当前没有可展示的日志记录，后续产生数据后会展示在这里。" />
      <el-table v-else :data="logs" v-loading="loading" stripe empty-text="暂无日志">
        <el-table-column label="类型" width="100"><template #default="{ row }"><el-tag round effect="light">{{ row.logType }}</el-tag></template></el-table-column>
        <el-table-column label="操作人" width="120"><template #default="{ row }">{{ row.operator }}</template></el-table-column>
        <el-table-column label="动作" min-width="180"><template #default="{ row }">{{ row.action }}</template></el-table-column>
        <el-table-column label="结果" width="100"><template #default="{ row }"><el-tag :type="row.result === 'success' ? 'success' : 'warning'" round effect="light">{{ row.result }}</el-tag></template></el-table-column>
        <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ row.time }}</template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryLogs, exportLogs } from '@/api/system';
import { downloadFile } from '@/api/resource';
type LogRow = Record<string, any>;
const logs = ref<LogRow[]>([]);
const { confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
async function loadLogs() { await runLoad(async () => { const result = await queryLogs({ pageNum: 1, pageSize: 20 }); logs.value = result.list; }, '全量日志加载失败，请稍后重试'); }
async function exportCurrent() { const confirmed = await confirmAction('确认导出全量日志归档吗？', '日志导出确认'); if (!confirmed) { return; } const result = await exportLogs({ scope: 'all' }); const download = await downloadFile({ fileKey: result.fileKey }); ElMessage.success(`导出任务已生成：${download.fileKey}`); }
onMounted(loadLogs);
</script>
