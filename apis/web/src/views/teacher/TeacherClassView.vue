<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">教师专属页</span><h1 class="page-hero__title">班级管理</h1><p class="page-hero__desc">支持教师创建专属班级、审批学生入班以及移除学生。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadClasses">刷新数据</el-button>
          <AuthorizedAction
            permission="class.create"
            type="primary"
            :disabled="!canCreateMore"
            @click="openCreateDialog"
          >
            创建班级
          </AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <MetricsOverview :items="metrics" />
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="班级列表" description="教师默认最多创建 1 个专属班级。" />
        <StatePanel v-if="loadError" type="error" title="班级列表加载失败" :description="loadError" action-text="重新加载" @action="loadClasses" />
        <el-table v-else :data="classes" v-loading="loading" stripe empty-text="暂无班级">
          <el-table-column label="班级名称" min-width="160"><template #default="{ row }">{{ row.className }}</template></el-table-column>
          <el-table-column label="班级码" width="110"><template #default="{ row }">{{ row.classCode }}</template></el-table-column>
          <el-table-column label="正式人数" width="100"><template #default="{ row }">{{ row.approvedMemberCount }}</template></el-table-column>
          <el-table-column label="待审核" width="100"><template #default="{ row }">{{ row.pendingMemberCount }}</template></el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <AuthorizedAction
                text
                permission="class.approveJoin"
                type="success"
                :disabled="!row.pendingStudents?.length"
                @click="approve(row)"
              >
                审批入班
              </AuthorizedAction>
              <AuthorizedAction
                text
                permission="class.removeStudent"
                type="danger"
                :disabled="!row.students?.length"
                @click="remove(row)"
              >
                移除学生
              </AuthorizedAction>
            </template>
          </el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="班级说明" description="教师可查看班级详情和班级码。">
          <template #extra>
            <span v-if="!canCreateMore" class="soft-tag soft-tag--warning">已达到 1 个班级上限</span>
          </template>
        </SectionHeader>
        <InfoPreviewCard v-if="classes.length" :badge="classes[0].classCode" :title="classes[0].className" :lines="classPreviewLines" />
        <StatePanel v-else type="empty" title="暂无班级说明" description="当前还没有班级，可新建后再开展入班审批。" />
      </article>
    </section>
    <el-dialog v-model="dialogVisible" title="创建班级" width="560px">
      <el-form label-position="top"><el-form-item label="班级名称"><el-input v-model="className" /></el-form-item><el-form-item label="班级说明"><el-input v-model="description" type="textarea" :rows="3" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><AuthorizedAction permission="class.create" type="primary" :loading="submitting" @click="submitCreate">保存</AuthorizedAction></template>
    </el-dialog>
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
import { queryClasses, createClass, approveJoinClass, removeStudent } from '@/api/class';
type ClassRow = Record<string, any>;
const submitting = ref(false);
const dialogVisible = ref(false);
const className = ref('教师新班级');
const description = ref('由教师页创建');
const classes = ref<ClassRow[]>([]);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const metrics = computed(() => [
  { label: '班级总数', value: classes.value.length },
  { label: '正式人数', value: classes.value.reduce((sum, item) => sum + Number(item.approvedMemberCount ?? 0), 0) },
  { label: '待审核', value: classes.value.reduce((sum, item) => sum + Number(item.pendingMemberCount ?? 0), 0) }
]);
const canCreateMore = computed(() => classes.value.length < 1);
const classPreviewLines = computed(() => {
  const current = classes.value[0];
  if (!current) {
    return [];
  }
  return [
    current.description,
    `正式人数：${current.approvedMemberCount}`,
    `待审核人数：${current.pendingMemberCount}`
  ];
});
async function loadClasses() {
  await runLoad(async () => {
    const result = await queryClasses({ pageNum: 1, pageSize: 20 });
    classes.value = result.list;
  }, '教师班级列表加载失败，请稍后重试');
}
function openCreateDialog() {
  if (!ensure(canCreateMore.value, '教师默认最多创建 1 个专属班级')) {
    return;
  }
  dialogVisible.value = true;
}
async function submitCreate() {
  const nextClassName = className.value.trim();
  const nextDescription = description.value.trim();
  if (!ensure(nextClassName, '请输入班级名称')) {
    return;
  }
  if (!ensure(nextDescription, '请输入班级说明')) {
    return;
  }
  if (!ensure(canCreateMore.value, '教师默认最多创建 1 个专属班级')) {
    return;
  }
  submitting.value = true;
  try {
    await createClass({ className: nextClassName, description: nextDescription });
    dialogVisible.value = false;
    className.value = nextClassName;
    description.value = nextDescription;
    ElMessage.success('班级已创建');
    await loadClasses();
  } finally { submitting.value = false; }
}
async function approve(row: ClassRow) {
  if (!ensure(row.pendingStudents?.length, '当前没有待审批学生')) {
    return;
  }
  await approveJoinClass({ classId: row.classId, studentIds: [row.pendingStudents[0]] });
  ElMessage.success('已审批入班');
  await loadClasses();
}
async function remove(row: ClassRow) {
  const studentId = row.students?.[0];
  if (!ensure(studentId, '当前班级没有可移除学生')) {
    return;
  }
  const confirmed = await confirmAction(`确认将学生移出“${row.className}”吗？`, '移除学生确认');
  if (!confirmed) {
    return;
  }
  await removeStudent({ classId: row.classId, studentId });
  ElMessage.success('已移除学生');
  await loadClasses();
}
onMounted(loadClasses);
</script>
