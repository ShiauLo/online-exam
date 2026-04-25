<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">管理员专属页</span><h1 class="page-hero__title">试题审核</h1><p class="page-hero__desc">集中审核待审试题，并支持导出归档。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadQuestions">刷新数据</el-button>
          <AuthorizedAction permission="question.export" type="success" @click="exportCurrent">导出题库</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="待审试题" description="仅展示待审核试题。" />
        <StatePanel v-if="loadError" type="error" title="待审试题加载失败" :description="loadError" action-text="重新加载" @action="loadQuestions" />
        <el-table v-else :data="questions" v-loading="loading" stripe empty-text="暂无待审试题">
          <el-table-column label="题目内容" min-width="220"><template #default="{ row }">{{ row.content }}</template></el-table-column>
          <el-table-column label="出题人" width="120"><template #default="{ row }">{{ row.creatorName }}</template></el-table-column>
          <el-table-column label="分类" width="120"><template #default="{ row }">{{ row.categoryName }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag round effect="light">{{ row.auditStatus }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><AuthorizedAction text permission="question.audit" type="success" @click="approve(row)">审核通过</AuthorizedAction></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="审核提示" description="审核通过后，题目即可进入后续组卷流程。" />
        <InfoPreviewCard
          v-if="questions.length"
          badge="待审核"
          badge-class="soft-tag--warning"
          :title="questions[0].content"
          :lines="questionPreviewLines"
        />
        <StatePanel v-else type="empty" title="暂无待审试题" description="当前没有待审核试题，后续新增待审数据后会展示在这里。" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryQuestions, auditQuestion, exportQuestions } from '@/api/question';
import { downloadFile } from '@/api/resource';
type QuestionRow = Record<string, any>;
const questions = ref<QuestionRow[]>([]);
const { confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const questionPreviewLines = computed(() => {
  const current = questions.value[0];
  if (!current) {
    return [];
  }
  return [`分类：${current.categoryName}`, `出题人：${current.creatorName}`, `审核状态：${current.auditStatus}`];
});
async function loadQuestions() { await runLoad(async () => { const result = await queryQuestions({ pageNum: 1, pageSize: 20, auditStatus: 'pending' }); questions.value = result.list; }, '待审试题加载失败，请稍后重试'); }
async function approve(row: QuestionRow) { const confirmed = await confirmAction(`确认审核通过题目“${row.content}”吗？`, '试题审核确认'); if (!confirmed) { return; } await auditQuestion({ questionId: row.questionId, auditResult: 'approve' }); ElMessage.success('试题已审核通过'); await loadQuestions(); }
async function exportCurrent() { const confirmed = await confirmAction('确认导出当前待审题库吗？', '题库导出确认'); if (!confirmed) { return; } const result = await exportQuestions({ scope: 'audit' }); const download = await downloadFile({ fileKey: result.fileKey }); ElMessage.success(`导出任务已生成：${download.fileKey}`); }
onMounted(loadQuestions);
</script>
