<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">教师端核心页</span>
        <h1 class="page-hero__title">试卷管理</h1>
        <p class="page-hero__desc">
          支持手动组卷、自动组卷、发布到班级、终止、回收和导出试卷。
        </p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="试卷名称检索" @keyup.enter="loadPage" />
        </template>
        <template #actions>
          <el-button @click="loadPage">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="paper.createManual" type="primary" @click="openCreateDialog('manual')">手动组卷</AuthorizedAction>
          <AuthorizedAction permission="paper.createAuto" type="success" @click="openCreateDialog('auto')">自动组卷</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="试卷列表" description="已补齐试卷回收与导出接口，可直接在此完成发布闭环。">
          <template #extra>
            <span class="soft-tag soft-tag--success">共 {{ total }} 条</span>
          </template>
        </SectionHeader>

        <StatePanel v-if="loadError" type="error" title="试卷列表加载失败" :description="loadError" action-text="重新加载" @action="loadPage" />
        <el-table v-else :data="papers" v-loading="loading" stripe empty-text="暂无试卷">
          <el-table-column label="试卷名称" min-width="180">
            <template #default="{ row }">
              <div class="paper-cell">
                <strong>{{ row.paperName }}</strong>
                <p>题目数：{{ row.questionIds?.length ?? 0 }} / 及格线：{{ row.passScore }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="90">
            <template #default="{ row }">{{ row.examTime }} 分钟</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" round effect="light">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="发布范围" min-width="160">
            <template #default="{ row }">
              {{ publishLabel(row.publishScope) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="260">
            <template #default="{ row }">
              <div class="row-actions">
                <AuthorizedAction text permission="paper.publish" type="primary" @click="openPublishDialog(row)">发布</AuthorizedAction>
                <AuthorizedAction text permission="paper.terminate" type="warning" @click="terminate(row)">终止</AuthorizedAction>
                <AuthorizedAction text permission="paper.recycle" type="danger" @click="recycle(row)">回收</AuthorizedAction>
                <el-button text @click="previewPaper(row)">预览</el-button>
                <AuthorizedAction text permission="paper.export" type="success" @click="exportCurrent(row)">导出</AuthorizedAction>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <PaginationFooter :current-page="pageNum" :page-size="pageSize" :total="total" @change="handlePageChange" />
      </article>

      <article class="surface-section glass-card">
        <SectionHeader title="试卷预览" description="可查看试卷状态、题目数与发布班级范围。" />

        <InfoPreviewCard v-if="selectedPaper" class="detail-preview" :badge="statusLabel(selectedPaper.status)" :title="selectedPaper.paperName" :lines="selectedPaperLines" />
        <StatePanel v-else type="empty" title="暂无试卷预览" description="当前暂无试卷数据，可先创建试卷或切换查询条件。" />
        <div class="question-tags">
          <el-tag v-for="item in questionOptions" :key="item.questionId" round effect="plain">{{ item.content }}</el-tag>
        </div>
      </article>
    </section>

    <el-dialog v-model="createDialogVisible" :title="createMode === 'manual' ? '手动组卷' : '自动组卷'" width="700px">
      <el-form label-position="top">
        <el-form-item label="试卷名称">
          <el-input v-model="paperForm.paperName" />
        </el-form-item>
        <FormGrid>
          <el-form-item label="考试时长">
            <el-input-number v-model="paperForm.examTime" :min="30" :max="180" />
          </el-form-item>
          <el-form-item label="及格线">
            <el-input-number v-model="paperForm.passScore" :min="0" :max="100" />
          </el-form-item>
        </FormGrid>
        <el-form-item v-if="createMode === 'manual'" label="题目选择">
          <el-select v-model="paperForm.questionIds" multiple filterable placeholder="请选择题目">
            <el-option v-for="item in questionOptions" :key="item.questionId" :label="item.content" :value="item.questionId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <AuthorizedAction :permission="createMode === 'manual' ? 'paper.createManual' : 'paper.createAuto'" type="primary" :loading="submitting" @click="submitCreate">保存</AuthorizedAction>
      </template>
    </el-dialog>

    <el-dialog v-model="publishDialogVisible" title="发布试卷" width="620px">
      <el-form label-position="top">
        <el-form-item label="发布班级">
          <el-select v-model="publishClassIds" multiple placeholder="请选择要发布的班级">
            <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="paper.publish" type="primary" :loading="submitting" @click="submitPublish">确认发布</AuthorizedAction>
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
import { queryPapers, createManualPaper, createAutoPaper, publishPaper, terminatePaper, recyclePaper, exportPaper } from '@/api/paper';
import { queryQuestions } from '@/api/question';
import { queryClasses } from '@/api/class';
import { downloadFile } from '@/api/resource';

type PaperRow = Record<string, any>;

const submitting = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const papers = ref<PaperRow[]>([]);
const selectedPaper = ref<PaperRow | null>(null);
const questionOptions = ref<PaperRow[]>([]);
const classOptions = ref<PaperRow[]>([]);
const createDialogVisible = ref(false);
const publishDialogVisible = ref(false);
const createMode = ref<'manual' | 'auto'>('manual');
const currentPaperId = ref('');
const publishClassIds = ref<string[]>([]);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const paperForm = reactive({
  paperName: '',
  examTime: 90,
  passScore: 60,
  questionIds: [] as string[]
});

const metrics = computed(() => [
  { label: '试卷总数', value: total.value },
  { label: '已发布', value: papers.value.filter((item) => item.status === 'published').length },
  { label: '已回收', value: papers.value.filter((item) => item.status === 'recycled').length }
]);
const selectedPaperLines = computed(() => {
  if (!selectedPaper.value) {
    return [];
  }
  return [
    `试卷时长：${selectedPaper.value.examTime} 分钟`,
    `及格线：${selectedPaper.value.passScore} 分`,
    `题目数量：${selectedPaper.value.questionIds?.length ?? 0}`,
    `发布范围：${publishLabel(selectedPaper.value.publishScope)}`
  ];
});

function statusLabel(value: string) {
  return value === 'published' ? '已发布' : value === 'terminated' ? '已终止' : value === 'recycled' ? '已回收' : '草稿';
}

function statusType(value: string) {
  return value === 'published' ? 'success' : value === 'terminated' ? 'warning' : value === 'recycled' ? 'danger' : 'info';
}

function publishLabel(scope: string[]) {
  return Array.isArray(scope) && scope.length ? scope.join('、') : '未发布';
}

function resetCreateForm() {
  paperForm.paperName = '';
  paperForm.examTime = 90;
  paperForm.passScore = 60;
  paperForm.questionIds = [];
}

async function loadDependencies() {
  const [questionResult, classResult] = await Promise.all([
    queryQuestions({ pageNum: 1, pageSize: 50 }),
    queryClasses({ pageNum: 1, pageSize: 20 })
  ]);
  questionOptions.value = questionResult.list;
  classOptions.value = classResult.list;
}

async function loadPapers() {
  const result = await queryPapers({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    keyword: keyword.value
  });
  papers.value = result.list;
  total.value = result.total;
  if (!papers.value.length) {
    selectedPaper.value = null;
  } else if (!selectedPaper.value || !papers.value.some((item) => item.paperId === selectedPaper.value?.paperId)) {
    selectedPaper.value = papers.value[0];
  }
}

async function loadPage() {
  await runLoad(async () => {
    await loadDependencies();
    await loadPapers();
  }, '教师试卷数据加载失败，请稍后重试');
}

function handlePageChange(value: number) {
  pageNum.value = value;
  loadPage();
}

function resetFilters() {
  keyword.value = '';
  pageNum.value = 1;
  loadPage();
}

function openCreateDialog(mode: 'manual' | 'auto') {
  createMode.value = mode;
  resetCreateForm();
  paperForm.paperName = mode === 'manual' ? '教师手动组卷' : '教师自动组卷';
  createDialogVisible.value = true;
}

async function submitCreate() {
  const paperName = paperForm.paperName.trim();
  if (!ensure(paperName, '请输入试卷名称')) {
    return;
  }
  if (!ensure(createMode.value === 'auto' || paperForm.questionIds.length, '手动组卷至少选择一道题目')) {
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      paperName,
      examTime: paperForm.examTime,
      passScore: paperForm.passScore,
      questionIds: paperForm.questionIds
    };
    if (createMode.value === 'manual') {
      await createManualPaper(payload);
      ElMessage.success('手动组卷成功');
    } else {
      await createAutoPaper(payload);
      ElMessage.success('自动组卷成功');
    }
    createDialogVisible.value = false;
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

function openPublishDialog(row: PaperRow) {
  currentPaperId.value = row.paperId;
  publishClassIds.value = Array.isArray(row.publishScope) ? [...row.publishScope] : [];
  publishDialogVisible.value = true;
}

async function submitPublish() {
  if (!ensure(currentPaperId.value, '未找到要发布的试卷')) {
    return;
  }
  if (!ensure(publishClassIds.value.length, '请至少选择一个发布班级')) {
    return;
  }
  const confirmed = await confirmAction('确认发布当前试卷到所选班级吗？', '试卷发布确认');
  if (!confirmed) {
    return;
  }
  submitting.value = true;
  try {
    await publishPaper({
      paperId: currentPaperId.value,
      classIds: publishClassIds.value
    });
    publishDialogVisible.value = false;
    ElMessage.success('试卷已发布');
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

async function terminate(row: PaperRow) {
  const confirmed = await confirmAction(`确认终止试卷“${row.paperName}”吗？`, '终止试卷确认');
  if (!confirmed) {
    return;
  }
  await terminatePaper({ paperId: row.paperId });
  ElMessage.success('试卷已终止');
  await loadPage();
}

async function recycle(row: PaperRow) {
  const confirmed = await confirmAction(`确认回收试卷“${row.paperName}”吗？`, '回收试卷确认');
  if (!confirmed) {
    return;
  }
  await recyclePaper({ paperId: row.paperId });
  ElMessage.success('试卷已回收');
  await loadPage();
}

async function exportCurrent(row: PaperRow) {
  const confirmed = await confirmAction(`确认导出试卷“${row.paperName}”吗？`, '导出试卷确认');
  if (!confirmed) {
    return;
  }
  const result = await exportPaper({ paperId: row.paperId });
  const download = await downloadFile({ fileKey: result.fileKey });
  ElMessage.success(`导出任务已生成：${download.fileKey}`);
}

function previewPaper(row: PaperRow) {
  selectedPaper.value = row;
}

onMounted(loadPage);
</script>

<style scoped>
.row-actions,.question-tags{display:flex;flex-wrap:wrap;gap:10px}
.toolbar-search{width:240px}
.paper-cell p{margin:0;color:var(--text-secondary);line-height:1.7}
.paper-cell strong{display:block;margin-bottom:6px}
.detail-preview{margin-bottom:18px}
@media (max-width:768px){.toolbar-search{width:100%}}
</style>
