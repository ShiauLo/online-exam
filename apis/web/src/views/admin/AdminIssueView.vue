<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">管理员专属页</span><h1 class="page-hero__title">业务问题</h1><p class="page-hero__desc">处理业务类问题单，支持接单、转派、关闭和流程跟踪。</p></div>
      <PageToolbar>
        <template #filters>
          <span class="soft-tag" :class="realtimeStatusClass">{{ realtimeStatusText }}</span>
          <span v-if="latestMessage" class="soft-tag soft-tag--warning">{{ latestMessage }}</span>
        </template>
        <template #actions>
          <el-button @click="loadIssues">刷新数据</el-button>
        </template>
      </PageToolbar>
    </section>
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="问题列表" description="管理员负责处理业务类问题。" />
        <StatePanel v-if="loadError" type="error" title="问题列表加载失败" :description="loadError" action-text="重新加载" @action="loadIssues" />
        <el-table v-else :data="issues" v-loading="loading" stripe empty-text="暂无问题">
          <el-table-column label="标题" min-width="180"><template #default="{ row }">{{ row.title }}</template></el-table-column>
          <el-table-column label="发起人" width="120"><template #default="{ row }">{{ row.reporterName }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'CLOSED' ? 'success' : 'warning'" round effect="light">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" min-width="220"><template #default="{ row }"><AuthorizedAction text permission="issue.handle" type="primary" @click="takeIssue(row)">接单</AuthorizedAction><AuthorizedAction text permission="issue.transfer" type="warning" @click="transfer(row)">转派</AuthorizedAction><AuthorizedAction text permission="issue.close" type="danger" @click="closeCurrent(row)">关闭</AuthorizedAction><el-button text @click="previewIssue(row)">跟踪</el-button></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="处理时间线" description="展示业务问题的处理过程。" />
        <TimelineList v-if="timelineItems.length" :items="timelineItems" />
        <StatePanel v-else type="empty" title="暂无时间线" description="请选择一条问题查看时间线。" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import TimelineList from '@/components/TimelineList.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { useIssueRealtime } from '@/composables/useIssueRealtime';
import { useAuthStore } from '@/stores/auth';
import { queryIssues, handleIssue, transferIssue, closeIssue } from '@/api/issue';
type IssueRow = Record<string, any>;
const issues = ref<IssueRow[]>([]);
const authStore = useAuthStore();
const { confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const { timeline, latestMessage, realtimeStatusText, realtimeStatusClass, previewIssue, connectIssueRealtime } = useIssueRealtime(loadIssues);
const timelineItems = computed(() =>
  timeline.value.map((item, index) => ({
    key: `${item.time}-${item.title}-${index}`,
    tag: item.time,
    title: item.title,
    description: item.desc
  }))
);
async function loadIssues() { await runLoad(async () => { const result = await queryIssues({ pageNum: 1, pageSize: 20 }); issues.value = result.list; }, '管理员问题列表加载失败，请稍后重试'); }
async function takeIssue(row: IssueRow) { const confirmed = await confirmAction(`确认接单处理“${row.title}”吗？`, '接单确认'); if (!confirmed) { return; } await handleIssue({ issueId: row.issueId, result: '管理员已接单' }); ElMessage.success('问题已接单'); await loadIssues(); }
async function transfer(row: IssueRow) {
  const confirmed = await confirmAction(`确认将“${row.title}”转派给另一位管理员吗？`, '转派确认');
  if (!confirmed) {
    return;
  }
  const currentUserId = String(authStore.user?.accountId ?? '');
  const targetHandlerId = currentUserId === '1' ? '2' : '1';
  await transferIssue({ issueId: row.issueId, toHandlerId: targetHandlerId, reason: '转交另一位管理员继续处理' });
  ElMessage.success('问题已转派');
  await loadIssues();
}
async function closeCurrent(row: IssueRow) { const confirmed = await confirmAction(`确认关闭问题“${row.title}”吗？`, '关闭问题确认'); if (!confirmed) { return; } await closeIssue({ issueId: row.issueId, comment: '管理员确认问题已解决' }); ElMessage.success('问题已关闭'); await loadIssues(); }
onMounted(async () => { await loadIssues(); connectIssueRealtime(); });
</script>
