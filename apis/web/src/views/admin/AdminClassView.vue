<template>
  <div>
    <section class="page-hero">
      <div><span class="soft-tag">管理员专属页</span><h1 class="page-hero__title">班级管理</h1><p class="page-hero__desc">统一管理班级台账，支持新增、编辑、删除和批量导入。</p></div>
      <PageToolbar>
        <template #actions>
          <el-button @click="loadClasses">刷新数据</el-button>
          <AuthorizedAction permission="class.create" type="primary" @click="openCreate">新增班级</AuthorizedAction>
          <AuthorizedAction permission="class.import" type="success" @click="runImport">导入班级</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>
    <section class="surface-section glass-card">
      <SectionHeader title="班级台账" description="适用于跨教师协调和批量导入场景。" />
      <StatePanel v-if="loadError" type="error" title="班级台账加载失败" :description="loadError" action-text="重新加载" @action="loadClasses" />
      <StatePanel v-else-if="!loading && !classes.length" type="empty" title="暂无班级台账" description="当前没有可管理的班级记录，可先新增班级或导入台账。" />
      <el-table v-else :data="classes" v-loading="loading" stripe empty-text="暂无班级">
        <el-table-column label="班级名称" min-width="160"><template #default="{ row }">{{ row.className }}</template></el-table-column>
        <el-table-column label="负责教师" width="120"><template #default="{ row }">{{ row.teacherName }}</template></el-table-column>
        <el-table-column label="班级码" width="110"><template #default="{ row }">{{ row.classCode }}</template></el-table-column>
        <el-table-column label="正式人数" width="100"><template #default="{ row }">{{ row.approvedMemberCount }}</template></el-table-column>
        <el-table-column label="操作" width="180"><template #default="{ row }"><AuthorizedAction text permission="class.update" type="warning" @click="openEdit(row)">编辑</AuthorizedAction><AuthorizedAction text permission="class.delete" type="danger" @click="removeCurrent(row)">删除</AuthorizedAction></template></el-table-column>
      </el-table>
    </section>
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑班级' : '新增班级'" width="560px">
      <el-form label-position="top"><el-form-item label="班级名称"><el-input v-model="className" /></el-form-item><el-form-item label="班级说明"><el-input v-model="description" type="textarea" :rows="3" /></el-form-item></el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><AuthorizedAction :permission="editingId ? 'class.update' : 'class.create'" type="primary" :loading="submitting" @click="submit">保存</AuthorizedAction></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryClasses, createClass, updateClass, deleteClass, importClasses } from '@/api/class';
type ClassRow = Record<string, any>;
const submitting = ref(false); const dialogVisible = ref(false); const editingId = ref(''); const className = ref('管理员新班级'); const description = ref('由管理员页维护'); const classes = ref<ClassRow[]>([]);
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
async function loadClasses() { await runLoad(async () => { const result = await queryClasses({ pageNum: 1, pageSize: 20 }); classes.value = result.list; }, '管理员班级台账加载失败，请稍后重试'); }
function openCreate() { editingId.value = ''; className.value = '管理员新班级'; description.value = '由管理员页维护'; dialogVisible.value = true; }
function openEdit(row: ClassRow) { editingId.value = row.classId; className.value = row.className; description.value = row.description; dialogVisible.value = true; }
async function submit() { const nextClassName = className.value.trim(); const nextDescription = description.value.trim(); if (!ensure(nextClassName, '请输入班级名称')) { return; } if (!ensure(nextDescription, '请输入班级说明')) { return; } submitting.value = true; try { if (editingId.value) { await updateClass({ classId: editingId.value, className: nextClassName, description: nextDescription }); ElMessage.success('班级已更新'); } else { await createClass({ className: nextClassName, description: nextDescription }); ElMessage.success('班级已创建'); } dialogVisible.value = false; await loadClasses(); } finally { submitting.value = false; } }
async function removeCurrent(row: ClassRow) { const confirmed = await confirmAction(`确认删除班级“${row.className}”吗？`, '删除班级确认'); if (!confirmed) { return; } await deleteClass({ classId: row.classId }); ElMessage.success('班级已删除'); await loadClasses(); }
async function runImport() { const confirmed = await confirmAction('确认执行班级批量导入吗？', '导入班级确认'); if (!confirmed) { return; } const result = await importClasses({ batchName: '管理员导入班级' }); ElMessage.success(`导入完成，成功 ${result.successCount} 条`); await loadClasses(); }
onMounted(loadClasses);
</script>
