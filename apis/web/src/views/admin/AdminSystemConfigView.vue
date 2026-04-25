<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">管理员专属页</span><h1 class="page-hero__title">系统配置</h1><p class="page-hero__desc">维护系统配置项和基础告警阈值。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadData">刷新数据</el-button>
        </template>
      </PageToolbar>
    </section>
    <StatePanel v-if="loadError" type="error" title="系统配置加载失败" :description="loadError" action-text="重新加载" @action="loadData" />
    <StatePanel v-else-if="!loading && !configs.length && !alarms.length" type="empty" title="暂无系统配置" description="当前没有可展示的配置项和告警阈值，请稍后再试。" />
    <section v-else class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="配置项" description="支持修改配置值。" />
        <el-table :data="configs" v-loading="loading" stripe empty-text="暂无配置">
          <el-table-column label="配置键" min-width="220"><template #default="{ row }">{{ row.configKey }}</template></el-table-column>
          <el-table-column label="分类" width="110"><template #default="{ row }"><el-tag round effect="light">{{ row.category }}</el-tag></template></el-table-column>
          <el-table-column label="当前值" width="120"><template #default="{ row }">{{ row.configValue }}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><AuthorizedAction text permission="system.config.update" type="warning" @click="updateCurrent(row)">修改</AuthorizedAction></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="告警阈值" description="支持更新告警阈值和通知对象。" />
        <el-table :data="alarms" v-loading="loading" stripe empty-text="暂无告警配置">
          <el-table-column label="告警类型" min-width="150"><template #default="{ row }">{{ row.alarmType }}</template></el-table-column>
          <el-table-column label="阈值" width="130"><template #default="{ row }">{{ row.threshold }}</template></el-table-column>
          <el-table-column label="通知对象" min-width="160"><template #default="{ row }">{{ (row.recipients ?? []).join('、') }}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><AuthorizedAction text permission="system.alarm.setting" type="warning" @click="updateAlarm(row)">修改</AuthorizedAction></template></el-table-column>
        </el-table>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryConfigs, updateConfig, queryAlarms, updateAlarmSetting } from '@/api/system';
type Row = Record<string, any>;
const configs = ref<Row[]>([]); const alarms = ref<Row[]>([]);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
async function loadData() { await runLoad(async () => { const [configResult, alarmResult] = await Promise.all([queryConfigs({ pageNum: 1, pageSize: 20 }), queryAlarms({ pageNum: 1, pageSize: 20 })]); configs.value = configResult.list; alarms.value = alarmResult.list; }, '系统配置数据加载失败，请稍后重试'); }
async function updateCurrent(row: Row) { try { const result = await ElMessageBox.prompt('请输入新的配置值', '修改配置', { inputValue: String(row.configValue ?? '') }); const nextValue = String(result.value ?? '').trim(); if (!ensure(nextValue, '配置值不能为空')) { return; } const confirmed = await confirmAction(`确认更新配置“${row.configKey}”吗？`, '配置修改确认'); if (!confirmed) { return; } await updateConfig({ configKey: row.configKey, configValue: nextValue }); ElMessage.success('配置已更新'); await loadData(); } catch {} }
async function updateAlarm(row: Row) { try { const result = await ElMessageBox.prompt('请输入新的告警阈值', '修改告警阈值', { inputValue: String(row.threshold ?? '') }); const nextValue = String(result.value ?? '').trim(); if (!ensure(nextValue, '告警阈值不能为空')) { return; } const confirmed = await confirmAction(`确认更新告警“${row.alarmType}”的阈值吗？`, '告警阈值确认'); if (!confirmed) { return; } await updateAlarmSetting({ alarmType: row.alarmType, threshold: nextValue, recipients: row.recipients }); ElMessage.success('告警阈值已更新'); await loadData(); } catch {} }
onMounted(loadData);
</script>
