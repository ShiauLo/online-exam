<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">运维专属页</span>
        <h1 class="page-hero__title">数据安全中心</h1>
        <p class="page-hero__desc">运维只负责发起备份和查看状态，不开放恢复入口。</p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-select v-model="backupType" class="toolbar-select" clearable placeholder="备份类型">
            <el-option label="全量备份" value="full" />
            <el-option label="增量备份" value="incremental" />
            <el-option label="配置备份" value="config" />
            <el-option label="审计备份" value="audit" />
          </el-select>
        </template>
        <template #actions>
          <el-button @click="loadBackups">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="system.data.backup" type="primary" @click="runBackupAction">
            发起备份
          </AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="surface-section glass-card">
      <SectionHeader title="备份记录" description="运维侧只能备份不能恢复。">
        <template #extra>
          <span class="soft-tag soft-tag--warning">禁止恢复</span>
        </template>
      </SectionHeader>
      <StatePanel v-if="loadError" type="error" title="备份记录加载失败" :description="loadError" action-text="重新加载" @action="loadBackups" />
      <StatePanel v-else-if="!loading && !backups.length" type="empty" title="暂无备份记录" description="当前没有符合条件的备份记录，可调整筛选条件后重试。" />
      <el-table v-else :data="backups" v-loading="loading" stripe empty-text="暂无备份">
        <el-table-column label="备份编号" min-width="130"><template #default="{ row }">{{ row.backupId }}</template></el-table-column>
        <el-table-column label="类型" width="110"><template #default="{ row }"><el-tag round effect="light">{{ row.backupType }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'RUNNING' ? 'warning' : 'danger'" round effect="light">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column label="生命周期" min-width="120"><template #default="{ row }">{{ row.lifecycleStage }}</template></el-table-column>
        <el-table-column label="更新时间" min-width="170"><template #default="{ row }">{{ row.updateTime }}</template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryBackups, runBackup } from '@/api/system';
type BackupRow = Record<string, any>;
const backupType = ref('');
const backups = ref<BackupRow[]>([]);
const { confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '备份总数', value: backups.value.length },
  { label: '运行中', value: backups.value.filter((item) => item.status === 'RUNNING').length },
  { label: '成功备份', value: backups.value.filter((item) => item.status === 'SUCCESS').length }
]);
async function loadBackups() {
  await runLoad(async () => {
    const result = await queryBackups({ pageNum: 1, pageSize: 20 });
    backups.value = backupType.value ? result.list.filter((item) => item.backupType === backupType.value) : result.list;
  }, '运维备份记录加载失败，请稍后重试');
}
function resetFilters() { backupType.value = ''; loadBackups(); }
async function runBackupAction() {
  const confirmed = await confirmAction('确认发起新的运维备份任务吗？', '运维备份确认');
  if (!confirmed) {
    return;
  }
  await runBackup({ backupType: backupType.value || 'incremental' });
  ElMessage.success('备份任务已发起');
  await loadBackups();
}
onMounted(loadBackups);
</script>

<style scoped>
.toolbar-select{width:150px}@media (max-width:768px){.toolbar-select{width:100%}}
</style>
