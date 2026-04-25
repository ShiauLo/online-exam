<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">教师端核心页</span>
        <h1 class="page-hero__title">试题管理</h1>
        <p class="page-hero__desc">
          管理个人题库、分类和审核状态，支持新增、编辑、停用切换、导入导出。
        </p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="题目内容 / 分类" @keyup.enter="loadPage" />
          <el-select v-model="auditStatus" class="toolbar-select" clearable placeholder="审核状态">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </template>
        <template #actions>
          <el-button @click="loadPage">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="question.create" type="primary" @click="openCreateDialog">新增试题</AuthorizedAction>
          <AuthorizedAction permission="question.import" @click="runImport">批量导入</AuthorizedAction>
          <AuthorizedAction permission="question.export" type="success" @click="runExport">导出题库</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="题库列表" description="仅展示当前教师可见题目，已补齐题目与分类查询接口。">
          <template #extra>
            <span class="soft-tag soft-tag--success">共 {{ total }} 条</span>
          </template>
        </SectionHeader>

        <StatePanel v-if="loadError" type="error" title="题库列表加载失败" :description="loadError" action-text="重新加载" @action="loadPage" />
        <el-table v-else :data="questions" v-loading="loading" stripe empty-text="暂无题目">
          <el-table-column label="题目内容" min-width="280">
            <template #default="{ row }">
              <div class="question-cell">
                <strong>{{ row.content }}</strong>
                <p>解析：{{ row.analysis }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="分类" min-width="120">
            <template #default="{ row }">
              <el-tag round effect="plain">{{ row.categoryName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="题型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'subjective' ? 'warning' : 'success'" round effect="light">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="difficultyType(row.difficulty)" round effect="light">{{ difficultyLabel(row.difficulty) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.auditStatus)" round effect="light">{{ auditLabel(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.isDisabled ? 'danger' : 'success'" round effect="light">{{ row.isDisabled ? '已停用' : '启用中' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="220">
            <template #default="{ row }">
              <div class="row-actions">
                <AuthorizedAction text permission="question.update" @click="openEditDialog(row)">编辑</AuthorizedAction>
                <AuthorizedAction text permission="question.toggleStatus" type="warning" @click="toggleStatus(row)">{{ row.isDisabled ? '启用' : '停用' }}</AuthorizedAction>
                <el-button text type="primary" @click="previewQuestion(row)">预览</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <PaginationFooter :current-page="pageNum" :page-size="pageSize" :total="total" @change="handlePageChange" />
      </article>

      <article class="surface-section glass-card">
        <SectionHeader title="分类与预览" description="左侧切换题目后，这里展示分类标签与题目详情。" />

        <div class="category-list">
          <el-tag v-for="item in categories" :key="item.categoryId" round effect="plain">
            {{ item.name }}{{ item.isPersonal ? ' / 个人' : ' / 公共' }}
          </el-tag>
        </div>

        <InfoPreviewCard v-if="selectedQuestion" class="detail-preview" :badge="typeLabel(selectedQuestion.type)" :title="selectedQuestion.content" :lines="selectedQuestionLines" />
        <StatePanel v-else type="empty" title="暂无题目详情" description="请选择一条题目查看详情。" />
      </article>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingQuestionId ? '编辑试题' : '新增试题'" width="680px">
      <el-form label-position="top">
        <el-form-item label="题目内容">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
        <FormGrid :columns="3">
          <el-form-item label="题型">
            <el-select v-model="form.type">
              <el-option label="单选题" value="single" />
              <el-option label="多选题" value="multi" />
              <el-option label="主观题" value="subjective" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.categoryId">
              <el-option v-for="item in categories" :key="item.categoryId" :label="item.name" :value="item.categoryId" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>
        </FormGrid>
        <el-form-item v-if="form.type !== 'subjective'" label="选项（每行一个）">
          <el-input v-model="optionText" type="textarea" :rows="4" placeholder="请输入选项，每行一个" />
        </el-form-item>
        <el-form-item label="标准答案">
          <el-input v-model="form.answer" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import FormGrid from '@/components/FormGrid.vue';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import PaginationFooter from '@/components/PaginationFooter.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import StatePanel from '@/components/StatePanel.vue';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryQuestions, queryCategories, createQuestion, updateQuestion, toggleQuestionStatus, importQuestions, exportQuestions } from '@/api/question';
import { downloadFile } from '@/api/resource';

type QuestionRow = Record<string, any>;

const submitting = ref(false);
const dialogVisible = ref(false);
const keyword = ref('');
const auditStatus = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const questions = ref<QuestionRow[]>([]);
const categories = ref<QuestionRow[]>([]);
const selectedQuestion = ref<QuestionRow | null>(null);
const editingQuestionId = ref('');
const optionText = ref('');
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const form = reactive({
  content: '',
  type: 'single',
  categoryId: 'qc-1',
  difficulty: 'medium',
  answer: '',
  analysis: ''
});

const metrics = computed(() => [
  { label: '题目总数', value: total.value },
  { label: '已通过', value: questions.value.filter((item) => item.auditStatus === 'approved').length },
  { label: '停用题目', value: questions.value.filter((item) => item.isDisabled).length }
]);
const selectedQuestionLines = computed(() => {
  if (!selectedQuestion.value) {
    return [];
  }
  return [
    `标准答案：${selectedQuestion.value.answer}`,
    `解析说明：${selectedQuestion.value.analysis}`,
    `引用限制：${selectedQuestion.value.referenceLocked ? '已被引用，关键字段受限' : '未被引用，可正常修改'}`
  ];
});

function typeLabel(value: string) {
  return value === 'single' ? '单选题' : value === 'multi' ? '多选题' : '主观题';
}

function difficultyLabel(value: string) {
  return value === 'easy' ? '简单' : value === 'hard' ? '困难' : '中等';
}

function difficultyType(value: string) {
  return value === 'easy' ? 'success' : value === 'hard' ? 'danger' : 'warning';
}

function auditLabel(value: string) {
  return value === 'approved' ? '已通过' : value === 'rejected' ? '已驳回' : '待审核';
}

function statusType(value: string) {
  return value === 'approved' ? 'success' : value === 'rejected' ? 'danger' : 'warning';
}

function resetForm() {
  editingQuestionId.value = '';
  form.content = '';
  form.type = 'single';
  form.categoryId = categories.value[0]?.categoryId ?? 'qc-1';
  form.difficulty = 'medium';
  form.answer = '';
  form.analysis = '';
  optionText.value = '';
}

async function loadCategories() {
  const result = await queryCategories({ pageNum: 1, pageSize: 20 });
  categories.value = result.list;
  if (!form.categoryId && categories.value.length) {
    form.categoryId = categories.value[0].categoryId;
  }
}

async function loadQuestions() {
  const result = await queryQuestions({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    keyword: keyword.value,
    auditStatus: auditStatus.value || undefined
  });
  questions.value = result.list;
  total.value = result.total;
  if (!questions.value.length) {
    selectedQuestion.value = null;
  } else if (!selectedQuestion.value || !questions.value.some((item) => item.questionId === selectedQuestion.value?.questionId)) {
    selectedQuestion.value = questions.value[0];
  }
}

async function loadPage() {
  await runLoad(async () => {
    await loadCategories();
    await loadQuestions();
  }, '教师题库数据加载失败，请稍后重试');
}

function handlePageChange(value: number) {
  pageNum.value = value;
  loadPage();
}

function resetFilters() {
  keyword.value = '';
  auditStatus.value = '';
  pageNum.value = 1;
  loadPage();
}

function openCreateDialog() {
  resetForm();
  dialogVisible.value = true;
}

function openEditDialog(row: QuestionRow) {
  editingQuestionId.value = row.questionId;
  form.content = row.content;
  form.type = row.type;
  form.categoryId = row.categoryId;
  form.difficulty = row.difficulty;
  form.answer = row.answer;
  form.analysis = row.analysis;
  optionText.value = Array.isArray(row.options) ? row.options.join('\n') : '';
  dialogVisible.value = true;
}

function previewQuestion(row: QuestionRow) {
  selectedQuestion.value = row;
}

async function submitForm() {
  const content = form.content.trim();
  const answer = form.answer.trim();
  const analysis = form.analysis.trim();
  const options = optionText.value.split('\n').map((item) => item.trim()).filter(Boolean);
  if (!ensure(content, '请输入题目内容')) {
    return;
  }
  if (!ensure(form.categoryId, '请选择题目分类')) {
    return;
  }
  if (!ensure(answer, '请输入标准答案')) {
    return;
  }
  if (!ensure(analysis, '请输入解析')) {
    return;
  }
  if (!ensure(form.type === 'subjective' || options.length >= 2, '客观题至少填写两个选项')) {
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      questionId: editingQuestionId.value || undefined,
      content,
      type: form.type,
      categoryId: form.categoryId,
      difficulty: form.difficulty,
      answer,
      analysis,
      options
    };
    if (editingQuestionId.value) {
      await updateQuestion(payload);
      ElMessage.success('试题已更新');
    } else {
      await createQuestion(payload);
      ElMessage.success('试题已创建');
    }
    dialogVisible.value = false;
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

async function toggleStatus(row: QuestionRow) {
  const confirmed = await confirmAction(
    row.isDisabled ? `确认启用题目“${row.content}”吗？` : `确认停用题目“${row.content}”吗？`,
    row.isDisabled ? '启用题目确认' : '停用题目确认'
  );
  if (!confirmed) {
    return;
  }
  await toggleQuestionStatus({
    questionId: row.questionId,
    isDisabled: !row.isDisabled
  });
  ElMessage.success(row.isDisabled ? '题目已启用' : '题目已停用');
  await loadPage();
}

async function runImport() {
  const confirmed = await confirmAction('确认执行教师题库批量导入吗？', '导入题库确认');
  if (!confirmed) {
    return;
  }
  const result = await importQuestions({ batchName: '教师批量导入' });
  ElMessage.success(`导入完成，成功 ${result.successCount} 条`);
  await loadPage();
}

async function runExport() {
  const confirmed = await confirmAction('确认导出当前教师可见题库吗？', '导出题库确认');
  if (!confirmed) {
    return;
  }
  const result = await exportQuestions({ scope: 'teacher' });
  const download = await downloadFile({ fileKey: result.fileKey });
  ElMessage.success(`导出任务已生成：${download.fileKey}`);
}

onMounted(loadPage);
</script>

<style scoped>
.row-actions,.category-list{display:flex;flex-wrap:wrap;gap:10px}
.toolbar-search{width:220px}
.toolbar-select{width:140px}
.question-cell strong{display:block;line-height:1.7}
.question-cell p{margin:8px 0 0;color:var(--text-secondary);line-height:1.7}
.category-list{margin-top:6px;margin-bottom:18px}
.detail-preview{margin-top:0}
@media (max-width:768px){.toolbar-search,.toolbar-select{width:100%}}
</style>
