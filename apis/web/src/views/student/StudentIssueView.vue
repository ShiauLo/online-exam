<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">学生专属页</span><h1 class="page-hero__title">问题申报</h1><p class="page-hero__desc">提交考试异常或业务问题，并跟踪处理进度。</p></div>
      <PageToolbar>
        <template #filters>
          <span class="soft-tag" :class="realtimeStatusClass">{{ realtimeStatusText }}</span>
          <span v-if="latestMessage" class="soft-tag soft-tag--warning">{{ latestMessage }}</span>
        </template>
        <template #actions>
          <el-button @click="loadIssues">刷新数据</el-button>
          <AuthorizedAction permission="issue.create" type="primary" @click="createIssueQuick">新建问题</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="问题列表" description="学生问题默认流转给教师处理。" />
        <StatePanel v-if="loadError" type="error" title="问题列表加载失败" :description="loadError" action-text="重新加载" @action="loadIssues" />
        <el-table v-else :data="issues" v-loading="loading" stripe empty-text="暂无问题">
          <el-table-column label="标题" min-width="180"><template #default="{ row }">{{ row.title }}</template></el-table-column>
          <el-table-column label="类型" width="100"><template #default="{ row }"><el-tag round effect="light">{{ row.type }}</el-tag></template></el-table-column>
          <el-table-column label="处理人" width="120"><template #default="{ row }">{{ row.currentHandlerName ?? row.handlerName ?? '-' }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'CLOSED' ? 'success' : 'warning'" round effect="light">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><el-button text type="primary" @click="previewIssue(row)">进度</el-button></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="处理时间线" description="查看问题单的处理过程。" />
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
import { queryIssues, createIssue } from '@/api/issue';
import { queryExams } from '@/api/examCore';
type IssueRow = Record<string, any>;
const issues = ref<IssueRow[]>([]);
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
async function loadIssues() { await runLoad(async () => { const result = await queryIssues({ pageNum: 1, pageSize: 20 }); issues.value = result.list; }, '学生问题列表加载失败，请稍后重试'); }
async function createIssueQuick() {
  const confirmed = await confirmAction('确认提交一条新的考试问题申报吗？', '问题申报确认');
  if (!confirmed) {
    return;
  }
  const examResult = await queryExams({ pageNum: 1, pageSize: 1 });
  const firstExam = examResult.list[0] ?? {};
  const examId = firstExam.examId;
  if (!examId) {
    ElMessage.warning('当前没有可关联的考试，暂时无法创建考试类问题');
    return;
  }
  await createIssue({ type: 'EXAM', title: '学生端新建问题', desc: '由学生页快速创建', examId });
  ElMessage.success('问题已创建');
  await loadIssues();
}
onMounted(async () => {
  await loadIssues();
  connectIssueRealtime();
});
</script>
