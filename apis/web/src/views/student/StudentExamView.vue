<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">学生专属页</span><h1 class="page-hero__title">我的考试</h1><p class="page-hero__desc">查看可参加考试、开始时间、状态和答题入口。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadExams">刷新列表</el-button>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="surface-section glass-card">
      <SectionHeader title="考试列表" description="进入考试后将进入实时答题页并启用自动保存。" />
      <StatePanel v-if="loadError" type="error" title="考试列表加载失败" :description="loadError" action-text="重新加载" @action="loadExams" />
      <el-table v-else :data="exams" v-loading="loading" stripe empty-text="暂无考试">
        <el-table-column label="考试名称" min-width="180"><template #default="{ row }">{{ row.examName }}</template></el-table-column>
        <el-table-column label="班级" min-width="160"><template #default="{ row }">{{ (row.classNames ?? []).join('、') }}</template></el-table-column>
        <el-table-column label="开始时间" min-width="170"><template #default="{ row }">{{ row.startTime }}</template></el-table-column>
        <el-table-column label="时长" width="100"><template #default="{ row }">{{ row.duration }} 分钟</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="row.status === 'underway' ? 'success' : 'warning'" round effect="light">{{ row.status }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120"><template #default="{ row }"><AuthorizedAction text permission="exam.enter" type="primary" @click="enterExam(row)">进入考试</AuthorizedAction></template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryExams } from '@/api/examCore';
type ExamRow = Record<string, any>;
const router = useRouter();
const exams = ref<ExamRow[]>([]);
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '考试总数', value: exams.value.length },
  { label: '进行中', value: exams.value.filter((item) => item.status === 'underway').length },
  { label: '待开始', value: exams.value.filter((item) => item.status === 'published').length }
]);
async function loadExams() {
  await runLoad(async () => {
    const result = await queryExams({ pageNum: 1, pageSize: 20 });
    exams.value = result.list;
  }, '学生考试列表加载失败，请稍后重试');
}
function enterExam(row: ExamRow) { router.push(`/student/exam/answer?examId=${row.examId}`); }
onMounted(loadExams);
</script>
