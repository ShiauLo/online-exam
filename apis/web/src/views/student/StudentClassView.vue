<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">学生专属页</span><h1 class="page-hero__title">我的班级</h1><p class="page-hero__desc">查看已加入班级、待审核班级和班级码入班入口，非强制班级可主动退出。</p></div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="classCode" class="toolbar-search" clearable placeholder="输入班级码申请加入" />
        </template>
        <template #actions>
          <AuthorizedAction permission="class.applyJoin" type="primary" @click="joinClass">
            申请入班
          </AuthorizedAction>
          <el-button @click="loadClasses">刷新列表</el-button>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="班级列表" description="展示学生当前可见班级以及待审核人数。" />
        <StatePanel v-if="loadError" type="error" title="班级列表加载失败" :description="loadError" action-text="重新加载" @action="loadClasses" />
        <el-table v-else :data="classes" v-loading="loading" stripe empty-text="暂无班级">
          <el-table-column label="班级名称" min-width="160"><template #default="{ row }">{{ row.className }}</template></el-table-column>
          <el-table-column label="教师" width="120"><template #default="{ row }">{{ row.teacherName }}</template></el-table-column>
          <el-table-column label="班级码" width="110"><template #default="{ row }">{{ row.classCode }}</template></el-table-column>
          <el-table-column label="正式人数" width="100"><template #default="{ row }">{{ row.approvedMemberCount }}</template></el-table-column>
          <el-table-column label="待审核" width="100"><template #default="{ row }">{{ row.pendingMemberCount }}</template></el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <AuthorizedAction
                text
                permission="class.quit"
                type="danger"
                :disabled="row.isMandatory"
                @click="quitCurrent(row)"
              >
                退出班级
              </AuthorizedAction>
            </template>
          </el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="班级说明" description="学生只能对非强制班级发起退出。" />
        <InfoPreviewCard v-if="classes.length" :badge="classes[0].isMandatory ? '强制班级' : '普通班级'" :title="classes[0].className" :lines="classPreviewLines" />
        <StatePanel v-else type="empty" title="暂无班级说明" description="当前暂无可查看班级，可通过班级码发起入班申请。" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryClasses, applyJoinClass, quitClass } from '@/api/class';
type ClassRow = Record<string, any>;
const classCode = ref('JAVA101');
const classes = ref<ClassRow[]>([]);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '班级总数', value: classes.value.length },
  { label: '正式成员数', value: classes.value.reduce((sum, item) => sum + Number(item.approvedMemberCount ?? 0), 0) },
  { label: '待审核总数', value: classes.value.reduce((sum, item) => sum + Number(item.pendingMemberCount ?? 0), 0) }
]);
const classPreviewLines = computed(() => {
  const current = classes.value[0];
  if (!current) {
    return [];
  }
  return [
    `课程说明：${current.description}`,
    `任课教师：${current.teacherName}`,
    `班级码：${current.classCode}`
  ];
});
async function loadClasses() {
  await runLoad(async () => {
    const result = await queryClasses({ pageNum: 1, pageSize: 20 });
    classes.value = result.list;
  }, '学生班级列表加载失败，请稍后重试');
}
async function joinClass() {
  const nextClassCode = classCode.value.trim();
  if (!ensure(nextClassCode, '请输入有效班级码')) {
    return;
  }
  await applyJoinClass({ classCode: nextClassCode });
  classCode.value = nextClassCode;
  ElMessage.success('已提交入班申请');
  await loadClasses();
}
async function quitCurrent(row: ClassRow) {
  if (!ensure(!row.isMandatory, '强制班级不可自行退出')) {
    return;
  }
  const confirmed = await confirmAction(`确认退出班级“${row.className}”吗？`, '退出班级确认');
  if (!confirmed) {
    return;
  }
  await quitClass({ classId: row.classId });
  ElMessage.success('已退出班级');
  await loadClasses();
}
onMounted(loadClasses);
</script>

<style scoped>
.toolbar-search{width:220px}@media (max-width:768px){.toolbar-search{width:100%}}
</style>
