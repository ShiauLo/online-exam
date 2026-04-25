<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">教师专属页</span><h1 class="page-hero__title">成绩管理</h1><p class="page-hero__desc">支持人工阅卷、成绩发布、导出和复核处理。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadScores">刷新数据</el-button>
          <AuthorizedAction permission="score.export" type="success" @click="exportCurrent">导出成绩</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="成绩列表" description="可查看总分、主观分和发布状态。" />
        <StatePanel v-if="loadError" type="error" title="成绩列表加载失败" :description="loadError" action-text="重新加载" @action="loadScores" />
        <el-table v-else :data="scores" v-loading="loading" stripe empty-text="暂无成绩">
          <el-table-column label="考试" min-width="160"><template #default="{ row }">{{ row.examName }}</template></el-table-column>
          <el-table-column label="学生" width="120"><template #default="{ row }">{{ row.studentName }}</template></el-table-column>
          <el-table-column label="总分" width="80"><template #default="{ row }">{{ row.totalScore }}</template></el-table-column>
          <el-table-column label="主观分" width="90"><template #default="{ row }">{{ row.subjectiveScore }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.publishStatus === 'published' ? 'success' : 'warning'" round effect="light">{{ row.publishStatus }}</el-tag></template></el-table-column>
          <el-table-column label="操作" min-width="220"><template #default="{ row }"><el-button text type="primary" @click="previewScore(row)">明细</el-button><AuthorizedAction text permission="score.manualScore" type="warning" @click="manual(row)">人工阅卷</AuthorizedAction><AuthorizedAction text permission="score.publish" type="success" :disabled="row.publishStatus === 'published'" @click="publish(row)">发布</AuthorizedAction><AuthorizedAction text permission="score.handleAppeal" type="danger" @click="appeal(row)">处理复核</AuthorizedAction></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="评分明细" description="显示当前成绩条目的题目得分明细。" />
        <TimelineList v-if="detailItems.length" :items="detailItems" />
        <StatePanel v-else type="empty" title="暂无评分明细" description="请选择一条成绩查看明细。" />
      </article>
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
import TimelineList from '@/components/TimelineList.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryScores, queryScoreDetail, manualScore, publishScores, exportScores, handleAppeal } from '@/api/score';
import { downloadFile } from '@/api/resource';
type ScoreRow = Record<string, any>;
const scores = ref<ScoreRow[]>([]);
const detailList = ref<ScoreRow[]>([]);
const { confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '成绩总数', value: scores.value.length },
  { label: '已发布', value: scores.value.filter((item) => item.publishStatus === 'published').length },
  { label: '平均分', value: scores.value.length ? (scores.value.reduce((sum, item) => sum + Number(item.totalScore ?? 0), 0) / scores.value.length).toFixed(1) : '0.0' }
]);
const detailItems = computed(() =>
  detailList.value.map((item, index) => ({
    key: item.questionId ?? index,
    tag: `得分 ${item.score}`,
    title: item.stem,
    description: item.comment || '暂无评语'
  }))
);
async function loadScores() { await runLoad(async () => { const result = await queryScores({ pageNum: 1, pageSize: 20 }); scores.value = result.list; }, '教师成绩列表加载失败，请稍后重试'); }
async function previewScore(row: ScoreRow) { const result = await queryScoreDetail({ examId: row.examId, studentId: row.studentId }); detailList.value = Array.isArray(result.detail) ? result.detail : []; }
async function manual(row: ScoreRow) {
  const confirmed = await confirmAction(`确认对“${row.studentName}”执行人工阅卷加分吗？`, '人工阅卷确认');
  if (!confirmed) {
    return;
  }
  await manualScore({ examId: row.examId, studentId: row.studentId, score: Number(row.subjectiveScore ?? 40) + 1 });
  ElMessage.success('主观分已更新');
  await loadScores();
}
async function publish(row: ScoreRow) {
  const confirmed = await confirmAction(`确认发布考试“${row.examName}”的成绩吗？`, '成绩发布确认');
  if (!confirmed) {
    return;
  }
  await publishScores({ examId: row.examId });
  ElMessage.success('成绩已发布');
  await loadScores();
}
async function appeal(row: ScoreRow) {
  const confirmed = await confirmAction(`确认处理“${row.studentName}”的复核申请吗？`, '复核处理确认');
  if (!confirmed) {
    return;
  }
  await handleAppeal({ appealId: row.scoreId });
  ElMessage.success('复核申请已处理');
  await loadScores();
}
async function exportCurrent() {
  const confirmed = await confirmAction('确认导出当前教师成绩数据吗？', '成绩导出确认');
  if (!confirmed) {
    return;
  }
  const result = await exportScores({ scope: 'teacher' });
  const download = await downloadFile({ fileKey: result.fileKey });
  ElMessage.success(`导出任务已生成：${download.fileKey}`);
}
onMounted(loadScores);
</script>
