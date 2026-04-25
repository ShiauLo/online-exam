<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">学生专属页</span><h1 class="page-hero__title">成绩查询</h1><p class="page-hero__desc">查看成绩、评分明细并发起主观题复核申请。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadScores">刷新数据</el-button>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="成绩列表" description="支持查看总分、发布状态和复核状态。" />
        <StatePanel v-if="loadError" type="error" title="成绩列表加载失败" :description="loadError" action-text="重新加载" @action="loadScores" />
        <el-table v-else :data="scores" v-loading="loading" stripe empty-text="暂无成绩">
          <el-table-column label="考试名称" min-width="180"><template #default="{ row }">{{ row.examName }}</template></el-table-column>
          <el-table-column label="班级" min-width="140"><template #default="{ row }">{{ row.className }}</template></el-table-column>
          <el-table-column label="总分" width="90"><template #default="{ row }">{{ row.totalScore }}</template></el-table-column>
          <el-table-column label="发布状态" width="110"><template #default="{ row }"><el-tag :type="row.publishStatus === 'published' ? 'success' : 'warning'" round effect="light">{{ row.publishStatus }}</el-tag></template></el-table-column>
          <el-table-column label="复核状态" width="110"><template #default="{ row }"><el-tag round effect="light">{{ row.recheckStatus || 'none' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="160"><template #default="{ row }"><el-button text type="primary" @click="previewScore(row)">明细</el-button><AuthorizedAction text permission="score.applyRecheck" type="warning" :disabled="row.publishStatus !== 'published'" @click="recheck(row)">复核</AuthorizedAction></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="评分明细" description="查看每题得分与教师评语。" />
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
import { queryScores, queryScoreDetail, applyScoreRecheck } from '@/api/score';
type ScoreRow = Record<string, any>;
const scores = ref<ScoreRow[]>([]);
const detailList = ref<ScoreRow[]>([]);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '成绩条数', value: scores.value.length },
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
async function loadScores() { await runLoad(async () => { const result = await queryScores({ pageNum: 1, pageSize: 20 }); scores.value = result.list; }, '学生成绩列表加载失败，请稍后重试'); }
async function previewScore(row: ScoreRow) { const result = await queryScoreDetail({ examId: row.examId, studentId: row.studentId }); detailList.value = Array.isArray(result.detail) ? result.detail : []; }
async function recheck(row: ScoreRow) {
  if (!ensure(row.publishStatus === 'published', '仅已发布成绩允许发起复核')) {
    return;
  }
  const confirmed = await confirmAction(`确认对“${row.examName}”发起主观题复核申请吗？`, '复核申请确认');
  if (!confirmed) {
    return;
  }
  await applyScoreRecheck({ examId: row.examId, reason: '学生申请主观题复核' });
  ElMessage.success('复核申请已提交');
  await loadScores();
}
onMounted(loadScores);
</script>
