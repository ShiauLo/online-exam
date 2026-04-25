<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">教师端核心页</span>
        <h1 class="page-hero__title">考试管理</h1>
        <p class="page-hero__desc">
          支持新建考试、更新考试参数、班级分发、暂停恢复和补考审批。
        </p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="考试名称检索" @keyup.enter="loadPage" />
          <el-select v-model="status" class="toolbar-select" clearable placeholder="考试状态">
            <el-option label="待开始" value="published" />
            <el-option label="进行中" value="underway" />
            <el-option label="已暂停" value="paused" />
            <el-option label="已结束" value="ended" />
          </el-select>
        </template>
        <template #actions>
          <el-button @click="loadPage">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="exam.create" type="primary" @click="openCreateDialog">
            新建考试
          </AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="考试列表" description="已补齐考试暂停/恢复接口，可直接对考试执行分发和状态切换。">
          <template #extra>
            <span class="soft-tag soft-tag--success">共 {{ total }} 条</span>
          </template>
        </SectionHeader>

        <StatePanel v-if="loadError" type="error" title="考试列表加载失败" :description="loadError" action-text="重新加载" @action="loadPage" />
        <el-table v-else :data="exams" v-loading="loading" stripe empty-text="暂无考试">
          <el-table-column label="考试名称" min-width="180">
            <template #default="{ row }">
              <div class="exam-cell">
                <strong>{{ row.examName }}</strong>
                <p>班级：{{ (row.classNames ?? []).join('、') || '未分发' }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="170">
            <template #default="{ row }">{{ row.startTime }}</template>
          </el-table-column>
          <el-table-column label="时长" width="90">
            <template #default="{ row }">{{ row.duration }} 分钟</template>
          </el-table-column>
          <el-table-column label="答题进度" width="120">
            <template #default="{ row }">{{ row.answeredCount }}/{{ row.totalCount }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" round effect="light">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="280">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button text @click="previewExam(row)">详情</el-button>
                <AuthorizedAction text permission="exam.distribute" type="primary" @click="openDistributeDialog(row)">
                  分发
                </AuthorizedAction>
                <AuthorizedAction text permission="exam.updateParams" type="warning" @click="openDurationDialog(row)">
                  改时长
                </AuthorizedAction>
                <AuthorizedAction text permission="exam.toggleStatus" type="danger" @click="toggleStatusAction(row)">
                  {{ row.isPaused ? '恢复' : '暂停' }}
                </AuthorizedAction>
                <AuthorizedAction text permission="exam.approveRetest" type="success" @click="approveRetestAction(row)">
                  补考审批
                </AuthorizedAction>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <PaginationFooter :current-page="pageNum" :page-size="pageSize" :total="total" @change="handlePageChange" />
      </article>

      <article class="surface-section glass-card">
        <SectionHeader title="考试详情" description="可查看当前考试投放范围、创建人和状态。" />

        <InfoPreviewCard v-if="selectedExam" class="detail-preview" :badge="statusLabel(selectedExam.status)" :title="selectedExam.examName" :lines="selectedExamLines" />
        <StatePanel v-else type="empty" title="暂无考试详情" description="当前暂无考试数据，可先创建考试或切换查询条件。" />
        <div class="class-tags">
          <el-tag v-for="item in classOptions" :key="item.classId" round effect="plain">{{ item.className }}</el-tag>
        </div>
      </article>
    </section>

    <el-dialog v-model="createDialogVisible" title="新建考试" width="720px">
      <el-form label-position="top">
        <el-form-item label="考试名称">
          <el-input v-model="examForm.examName" />
        </el-form-item>
        <FormGrid>
          <el-form-item label="试卷">
            <el-select v-model="examForm.paperId">
              <el-option v-for="item in paperOptions" :key="item.paperId" :label="item.paperName" :value="item.paperId" />
            </el-select>
          </el-form-item>
          <el-form-item label="考试时长">
            <el-input-number v-model="examForm.duration" :min="30" :max="180" />
          </el-form-item>
        </FormGrid>
        <el-form-item label="开始时间">
          <el-date-picker v-model="examForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="投放班级">
          <el-select v-model="examForm.classIds" multiple placeholder="请选择班级">
            <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="exam.create" type="primary" :loading="submitting" @click="submitCreate">保存</AuthorizedAction>
      </template>
    </el-dialog>

    <el-dialog v-model="distributeDialogVisible" title="分发考试" width="620px">
      <el-form label-position="top">
        <el-form-item label="投放班级">
          <el-select v-model="distributeClassIds" multiple placeholder="请选择班级">
            <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="distributeDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="exam.distribute" type="primary" :loading="submitting" @click="submitDistribute">确认分发</AuthorizedAction>
      </template>
    </el-dialog>

    <el-dialog v-model="durationDialogVisible" title="调整考试时长" width="460px">
      <el-form label-position="top">
        <el-form-item label="考试时长（分钟）">
          <el-input-number v-model="durationValue" :min="30" :max="180" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="durationDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="exam.updateParams" type="primary" :loading="submitting" @click="submitDuration">保存</AuthorizedAction>
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
import { queryExams, createExam, updateExamParams, distributeExam, toggleExamStatus, approveRetest } from '@/api/examCore';
import { queryPapers } from '@/api/paper';
import { queryClasses } from '@/api/class';

type ExamRow = Record<string, any>;

const submitting = ref(false);
const keyword = ref('');
const status = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const exams = ref<ExamRow[]>([]);
const selectedExam = ref<ExamRow | null>(null);
const classOptions = ref<ExamRow[]>([]);
const paperOptions = ref<ExamRow[]>([]);
const createDialogVisible = ref(false);
const distributeDialogVisible = ref(false);
const durationDialogVisible = ref(false);
const currentExamId = ref('');
const distributeClassIds = ref<string[]>([]);
const durationValue = ref(90);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const examForm = reactive({
  examName: '',
  paperId: '',
  duration: 90,
  startTime: '',
  classIds: [] as string[]
});

const metrics = computed(() => [
  { label: '考试总数', value: total.value },
  { label: '进行中', value: exams.value.filter((item) => item.status === 'underway').length },
  { label: '已暂停', value: exams.value.filter((item) => item.status === 'paused').length }
]);
const selectedExamLines = computed(() => {
  if (!selectedExam.value) {
    return [];
  }
  return [
    `试卷编号：${selectedExam.value.paperId}`,
    `开始时间：${selectedExam.value.startTime}`,
    `考试时长：${selectedExam.value.duration} 分钟`,
    `投放班级：${(selectedExam.value.classNames ?? []).join('、') || '未分发'}`,
    `创建人：${selectedExam.value.creatorName}`
  ];
});

function statusLabel(value: string) {
  if (value === 'underway') return '进行中';
  if (value === 'paused') return '已暂停';
  if (value === 'ended') return '已结束';
  return '待开始';
}

function statusType(value: string) {
  if (value === 'underway') return 'success';
  if (value === 'paused') return 'warning';
  if (value === 'ended') return 'info';
  return 'primary';
}

function resetForm() {
  examForm.examName = '';
  examForm.paperId = paperOptions.value[0]?.paperId ?? '';
  examForm.duration = 90;
  examForm.startTime = '';
  examForm.classIds = [];
}

async function loadDependencies() {
  const [paperResult, classResult] = await Promise.all([
    queryPapers({ pageNum: 1, pageSize: 20 }),
    queryClasses({ pageNum: 1, pageSize: 20 })
  ]);
  paperOptions.value = paperResult.list;
  classOptions.value = classResult.list;
  if (!examForm.paperId && paperOptions.value.length) {
    examForm.paperId = paperOptions.value[0].paperId;
  }
}

async function loadExams() {
  const result = await queryExams({
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    keyword: keyword.value,
    status: status.value || undefined
  });
  exams.value = result.list;
  total.value = result.total;
  if (!exams.value.length) {
    selectedExam.value = null;
  } else if (!selectedExam.value || !exams.value.some((item) => item.examId === selectedExam.value?.examId)) {
    selectedExam.value = exams.value[0];
  }
}

async function loadPage() {
  await runLoad(async () => {
    await loadDependencies();
    await loadExams();
  }, '教师考试数据加载失败，请稍后重试');
}

function handlePageChange(value: number) {
  pageNum.value = value;
  loadPage();
}

function resetFilters() {
  keyword.value = '';
  status.value = '';
  pageNum.value = 1;
  loadPage();
}

function previewExam(row: ExamRow) {
  selectedExam.value = row;
}

function openCreateDialog() {
  resetForm();
  examForm.examName = '教师创建考试';
  createDialogVisible.value = true;
}

async function submitCreate() {
  const examName = examForm.examName.trim();
  if (!ensure(examName, '请输入考试名称')) {
    return;
  }
  if (!ensure(examForm.paperId, '请选择试卷')) {
    return;
  }
  if (!ensure(examForm.startTime, '请选择开始时间')) {
    return;
  }
  if (!ensure(examForm.classIds.length, '请至少选择一个投放班级')) {
    return;
  }
  submitting.value = true;
  try {
    await createExam({
      examName,
      paperId: examForm.paperId,
      duration: examForm.duration,
      startTime: examForm.startTime,
      classIds: examForm.classIds
    });
    createDialogVisible.value = false;
    ElMessage.success('考试已创建');
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

function openDistributeDialog(row: ExamRow) {
  currentExamId.value = row.examId;
  distributeClassIds.value = Array.isArray(row.classIds) ? [...row.classIds] : [];
  distributeDialogVisible.value = true;
}

async function submitDistribute() {
  if (!ensure(currentExamId.value, '未找到要分发的考试')) {
    return;
  }
  if (!ensure(distributeClassIds.value.length, '请至少选择一个投放班级')) {
    return;
  }
  submitting.value = true;
  try {
    await distributeExam({
      examId: currentExamId.value,
      classIds: distributeClassIds.value
    });
    distributeDialogVisible.value = false;
    ElMessage.success('考试已分发');
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

function openDurationDialog(row: ExamRow) {
  currentExamId.value = row.examId;
  durationValue.value = row.duration;
  durationDialogVisible.value = true;
}

async function submitDuration() {
  if (!ensure(currentExamId.value, '未找到要调整的考试')) {
    return;
  }
  submitting.value = true;
  try {
    await updateExamParams({
      examId: currentExamId.value,
      duration: durationValue.value
    });
    durationDialogVisible.value = false;
    ElMessage.success('考试时长已更新');
    await loadPage();
  } finally {
    submitting.value = false;
  }
}

async function toggleStatusAction(row: ExamRow) {
  const confirmed = await confirmAction(
    row.isPaused ? `确认恢复考试“${row.examName}”吗？` : `确认暂停考试“${row.examName}”吗？`,
    row.isPaused ? '恢复考试确认' : '暂停考试确认'
  );
  if (!confirmed) {
    return;
  }
  await toggleExamStatus({
    examId: row.examId,
    isPaused: !row.isPaused
  });
  ElMessage.success(row.isPaused ? '考试已恢复' : '考试已暂停');
  await loadPage();
}

async function approveRetestAction(row: ExamRow) {
  const confirmed = await confirmAction(`确认通过“${row.examName}”的补考申请吗？`, '补考审批确认');
  if (!confirmed) {
    return;
  }
  await approveRetest({
    examId: row.examId,
    studentId: 'u-student-01'
  });
  ElMessage.success('补考申请已审批');
}

onMounted(loadPage);
</script>

<style scoped>
.row-actions,.class-tags{display:flex;flex-wrap:wrap;gap:10px}
.toolbar-search{width:220px}
.toolbar-select{width:140px}
.exam-cell p{margin:0;color:var(--text-secondary);line-height:1.7}
.exam-cell strong{display:block;margin-bottom:6px}
.detail-preview{margin-bottom:18px}
@media (max-width:768px){.toolbar-search,.toolbar-select{width:100%}}
</style>
