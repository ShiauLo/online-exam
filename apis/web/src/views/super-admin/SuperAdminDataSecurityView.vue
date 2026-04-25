<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag soft-tag--danger">超级管理员核心页</span>
        <h1 class="page-hero__title">数据安全中心</h1>
        <p class="page-hero__desc">
          负责查看备份记录并执行恢复流程。恢复动作必须按双验证码、双人授权设计。
        </p>
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
          <AuthorizedAction permission="system.data.restore" type="danger" @click="openRestoreDialog()">
            恢复数据
          </AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="备份记录" description="恢复前请确认目标备份、影响范围和双人验证码，Mock 验证码为 9527 与 3141。">
          <template #extra>
            <span class="soft-tag soft-tag--warning">高风险操作</span>
          </template>
        </SectionHeader>

        <StatePanel v-if="loadError" type="error" title="备份记录加载失败" :description="loadError" action-text="重新加载" @action="loadBackups" />
        <el-table v-else :data="backups" v-loading="loading" stripe empty-text="暂无备份">
          <el-table-column label="备份编号" min-width="130">
            <template #default="{ row }">
              <div class="backup-cell">
                <strong>{{ row.backupId }}</strong>
                <p>{{ row.lifecycleStage }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="backupTypeTag(row.backupType)" round effect="light">{{ backupTypeLabel(row.backupType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)" round effect="light">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" min-width="170">
            <template #default="{ row }">{{ row.updateTime }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="200">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button text type="primary" @click="previewBackup(row)">详情</el-button>
                <AuthorizedAction
                  text
                  permission="system.data.restore"
                  type="danger"
                  :disabled="!row.canRestore"
                  @click="openRestoreDialog(row)"
                >
                  恢复
                </AuthorizedAction>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="surface-section glass-card">
        <SectionHeader title="恢复说明" description="恢复动作由超级管理员执行，必须双人授权，运维侧无恢复权限。" />

        <InfoPreviewCard v-if="selectedBackup" badge="恢复前确认" badge-class="soft-tag--danger" :title="selectedBackup.backupId" :lines="selectedBackupLines" />
        <StatePanel v-else type="empty" title="暂无恢复说明" description="请选择一条备份记录查看说明。" />
      </article>
    </section>

    <el-dialog v-model="restoreDialogVisible" title="恢复数据" width="560px">
      <el-alert type="error" :closable="false" title="危险操作：恢复会覆盖当前业务数据，请先完成双人授权确认。" />
      <el-form label-position="top" class="restore-form">
        <el-form-item label="目标备份">
          <el-input :model-value="restoreBackupId" disabled />
        </el-form-item>
        <el-form-item label="第一位授权验证码">
          <el-input v-model="verifyCode1" />
        </el-form-item>
        <el-form-item label="第二位授权验证码">
          <el-input v-model="verifyCode2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="restoreDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="submitRestore">确认恢复</el-button>
      </template>
    </el-dialog>
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
import { queryBackups, runBackup, restoreBackup } from '@/api/system';

type BackupRow = Record<string, any>;

const submitting = ref(false);
const backupType = ref('');
const backups = ref<BackupRow[]>([]);
const selectedBackup = ref<BackupRow | null>(null);
const restoreDialogVisible = ref(false);
const restoreBackupId = ref('b-1');
const verifyCode1 = ref('9527');
const verifyCode2 = ref('3141');
const { ensure, confirmAction, confirmDanger } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();

const metrics = computed(() => [
  { label: '备份总数', value: backups.value.length },
  { label: '可恢复备份', value: backups.value.filter((item) => item.canRestore).length },
  { label: '运行中任务', value: backups.value.filter((item) => item.status === 'RUNNING').length }
]);
const selectedBackupLines = computed(() => {
  if (!selectedBackup.value) {
    return [];
  }
  return [
    `备份类型：${backupTypeLabel(selectedBackup.value.backupType)}`,
    `执行状态：${selectedBackup.value.status}`,
    `更新时间：${selectedBackup.value.updateTime}`,
    `生命周期：${selectedBackup.value.lifecycleStage}`,
    `恢复权限：${selectedBackup.value.canRestore ? '允许恢复' : '当前不可恢复'}`
  ];
});

function backupTypeLabel(value: string) {
  return value === 'full'
    ? '全量备份'
    : value === 'incremental'
      ? '增量备份'
      : value === 'config'
        ? '配置备份'
        : '审计备份';
}

function backupTypeTag(value: string) {
  return value === 'full' ? 'danger' : value === 'incremental' ? 'success' : value === 'config' ? 'warning' : 'info';
}

function statusTag(value: string) {
  return value === 'SUCCESS' ? 'success' : value === 'RUNNING' ? 'warning' : 'danger';
}

async function loadBackups() {
  await runLoad(async () => {
    const result = await queryBackups({
      pageNum: 1,
      pageSize: 20
    });
    const list = backupType.value
      ? result.list.filter((item) => item.backupType === backupType.value)
      : result.list;
    backups.value = list;
    if (!backups.value.length) {
      selectedBackup.value = null;
    } else if (!selectedBackup.value || !backups.value.some((item) => item.backupId === selectedBackup.value?.backupId)) {
      selectedBackup.value = backups.value[0];
    }
  }, '备份记录加载失败，请稍后重试');
}

function resetFilters() {
  backupType.value = '';
  loadBackups();
}

function previewBackup(row: BackupRow) {
  selectedBackup.value = row;
}

async function runBackupAction() {
  const confirmed = await confirmAction('确认立即发起新的数据备份任务吗？', '发起备份确认');
  if (!confirmed) {
    return;
  }
  await runBackup({
    backupType: backupType.value || 'incremental'
  });
  ElMessage.success('备份任务已发起');
  await loadBackups();
}

function openRestoreDialog(row?: BackupRow) {
  const targetBackup = row ?? selectedBackup.value ?? backups.value[0];
  if (!ensure(targetBackup, '当前没有可恢复的备份记录')) {
    return;
  }
  if (!ensure(targetBackup.canRestore, '该备份当前不可恢复')) {
    return;
  }
  restoreBackupId.value = targetBackup.backupId;
  verifyCode1.value = '9527';
  verifyCode2.value = '3141';
  restoreDialogVisible.value = true;
}

async function submitRestore() {
  if (!ensure(restoreBackupId.value, '请选择目标备份')) {
    return;
  }
  if (!ensure(verifyCode1.value.trim(), '请输入第一位授权验证码')) {
    return;
  }
  if (!ensure(verifyCode2.value.trim(), '请输入第二位授权验证码')) {
    return;
  }
  if (!ensure(verifyCode1.value.trim() !== verifyCode2.value.trim(), '双人授权验证码不能相同')) {
    return;
  }
  const confirmed = await confirmDanger(
    `确认以备份“${restoreBackupId.value}”执行数据恢复吗？该操作会覆盖当前业务数据。`,
    '数据恢复最终确认'
  );
  if (!confirmed) {
    return;
  }
  submitting.value = true;
  try {
    await restoreBackup({
      backupId: restoreBackupId.value,
      verifyCode1: verifyCode1.value.trim(),
      verifyCode2: verifyCode2.value.trim()
    });
    restoreDialogVisible.value = false;
    ElMessage.success('数据恢复流程已执行');
  } finally {
    submitting.value = false;
  }
}

onMounted(loadBackups);
</script>

<style scoped>
.row-actions{display:flex;flex-wrap:wrap;gap:10px}
.toolbar-select{width:150px}
.backup-cell p{margin:0;color:var(--text-secondary);line-height:1.7}
.backup-cell strong{display:block;margin-bottom:6px}
.restore-form{margin-top:16px}
@media (max-width:768px){.toolbar-select{width:100%}}
</style>
