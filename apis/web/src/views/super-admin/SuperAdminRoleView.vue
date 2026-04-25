<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">超级管理员核心页</span>
        <h1 class="page-hero__title">角色权限管理</h1>
        <p class="page-hero__desc">
          管理权限模板、配置权限集合并执行分配，适合高权限角色的集中治理。
        </p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="模板名称检索" @keyup.enter="applyFilter" />
        </template>
        <template #actions>
          <el-button @click="applyFilter">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="system.role.save" type="primary" @click="openTemplateDialog()">新增模板</AuthorizedAction>
          <AuthorizedAction permission="system.permission.assign" type="warning" @click="openAssignDialog()">分配权限</AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="权限模板" description="当前页以前端模板清单承接权限治理，并调用保存与分配接口完成落库动作。">
          <template #extra>
            <span class="soft-tag soft-tag--success">共 {{ filteredTemplates.length }} 条</span>
          </template>
        </SectionHeader>

        <el-table :data="filteredTemplates" stripe empty-text="暂无模板">
          <el-table-column label="模板名称" min-width="180">
            <template #default="{ row }">
              <div class="template-cell">
                <strong>{{ row.roleName }}</strong>
                <p>{{ row.permissionIds.length }} 项权限</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="权限数" width="100">
            <template #default="{ row }">{{ row.permissionCount }}</template>
          </el-table-column>
          <el-table-column label="失效时间" min-width="160">
            <template #default="{ row }">{{ row.expireTime || '长期有效' }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="200">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button text type="primary" @click="previewTemplate(row)">详情</el-button>
                <AuthorizedAction text permission="system.role.save" type="warning" @click="openTemplateDialog(row)">编辑</AuthorizedAction>
                <AuthorizedAction text permission="system.permission.assign" type="success" @click="openAssignDialog(row)">授权</AuthorizedAction>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </article>

      <article class="surface-section glass-card">
        <SectionHeader title="模板详情" description="显示模板包含的权限集合，并提示高风险操作范围。" />

        <InfoPreviewCard v-if="selectedTemplate" badge="高权限模板" badge-class="soft-tag--warning" :title="selectedTemplate.roleName" :lines="selectedTemplateLines">
          <div class="permission-list">
            <el-tag v-for="item in selectedTemplate.permissionIds" :key="item" round effect="plain">{{ item }}</el-tag>
          </div>
        </InfoPreviewCard>
        <StatePanel v-else type="empty" title="暂无模板详情" description="请选择一条模板查看详情。" />
      </article>
    </section>

    <el-dialog v-model="templateDialogVisible" :title="editingTemplateId ? '编辑模板' : '新增模板'" width="720px">
      <el-form label-position="top">
        <FormGrid>
          <el-form-item label="模板名称">
            <el-input v-model="templateForm.roleName" />
          </el-form-item>
          <el-form-item label="失效时间">
            <el-date-picker v-model="templateForm.expireTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable />
          </el-form-item>
        </FormGrid>
        <el-form-item label="权限集合">
          <el-select v-model="templateForm.permissionIds" multiple filterable placeholder="请选择权限">
            <el-option v-for="item in permissionOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="system.role.save" type="primary" :loading="submitting" @click="submitTemplate">保存</AuthorizedAction>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配权限" width="680px">
      <el-form label-position="top">
        <el-form-item label="目标模板">
          <el-select v-model="assignRoleId" placeholder="请选择模板">
            <el-option v-for="item in templates" :key="item.roleId" :label="item.roleName" :value="item.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权权限">
          <el-select v-model="assignPermissionIds" multiple filterable placeholder="请选择权限">
            <el-option v-for="item in permissionOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="system.permission.assign" type="primary" :loading="submitting" @click="submitAssign">确认分配</AuthorizedAction>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import FormGrid from '@/components/FormGrid.vue';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { saveRoleTemplate, assignPermission } from '@/api/system';

type TemplateRow = {
  roleId: string;
  roleName: string;
  permissionCount: number;
  expireTime?: string;
  permissionIds: string[];
};

const permissionOptions = [
  'system.log.export',
  'system.permission.assign',
  'system.role.save',
  'system.data.restore',
  'account.create',
  'account.audit',
  'account.freeze',
  'question.audit',
  'class.import'
];

const keyword = ref('');
const submitting = ref(false);
const templateDialogVisible = ref(false);
const assignDialogVisible = ref(false);
const editingTemplateId = ref('');
const selectedTemplate = ref<TemplateRow | null>(null);
const assignRoleId = ref('');
const assignPermissionIds = ref<string[]>([]);
const { ensure, confirmAction } = useActionGuard();
const templates = ref<TemplateRow[]>([
  {
    roleId: 'role-1',
    roleName: '教师标准权限',
    permissionCount: 6,
    permissionIds: ['question.audit', 'class.import', 'account.audit'],
    expireTime: ''
  },
  {
    roleId: 'role-2',
    roleName: '审计临时核查权限',
    permissionCount: 4,
    permissionIds: ['system.log.export', 'system.permission.assign'],
    expireTime: '2026-04-25 23:59:59'
  }
]);
const templateForm = reactive({
  roleName: '',
  expireTime: '',
  permissionIds: [] as string[]
});

const filteredTemplates = computed(() =>
  templates.value.filter((item) => item.roleName.includes(keyword.value.trim()))
);

const metrics = computed(() => [
  { label: '模板总数', value: templates.value.length },
  { label: '高危权限项', value: templates.value.reduce((sum, item) => sum + item.permissionIds.filter((permission) => permission.includes('restore') || permission.includes('assign')).length, 0) },
  { label: '临时模板', value: templates.value.filter((item) => Boolean(item.expireTime)).length }
]);
const selectedTemplateLines = computed(() => {
  if (!selectedTemplate.value) {
    return [];
  }
  return [
    `权限数量：${selectedTemplate.value.permissionCount}`,
    `失效时间：${selectedTemplate.value.expireTime || '长期有效'}`
  ];
});

function applyFilter() {
  if (!selectedTemplate.value && filteredTemplates.value.length) {
    selectedTemplate.value = filteredTemplates.value[0];
  }
}

function resetFilters() {
  keyword.value = '';
  applyFilter();
}

function previewTemplate(row: TemplateRow) {
  selectedTemplate.value = row;
}

function resetTemplateForm() {
  editingTemplateId.value = '';
  templateForm.roleName = '';
  templateForm.expireTime = '';
  templateForm.permissionIds = [];
}

function openTemplateDialog(row?: TemplateRow) {
  if (!row) {
    resetTemplateForm();
  } else {
    editingTemplateId.value = row.roleId;
    templateForm.roleName = row.roleName;
    templateForm.expireTime = row.expireTime || '';
    templateForm.permissionIds = [...row.permissionIds];
  }
  templateDialogVisible.value = true;
}

async function submitTemplate() {
  const roleName = templateForm.roleName.trim();
  if (!ensure(roleName, '请输入模板名称')) {
    return;
  }
  if (!ensure(templateForm.permissionIds.length, '请至少选择一项权限')) {
    return;
  }
  const confirmed = await confirmAction(editingTemplateId.value ? `确认保存模板“${roleName}”的变更吗？` : `确认创建模板“${roleName}”吗？`, editingTemplateId.value ? '模板更新确认' : '模板创建确认');
  if (!confirmed) {
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      roleId: editingTemplateId.value || undefined,
      roleName,
      permissionIds: templateForm.permissionIds,
      expireTime: templateForm.expireTime || undefined
    };
    const saved = await saveRoleTemplate(payload);
    const resolvedRoleId =
      saved.roleId ?? editingTemplateId.value ?? `role-${templates.value.length + 1}`;
    const normalized: TemplateRow = {
      roleId: String(resolvedRoleId),
      roleName: String(saved.roleName ?? templateForm.roleName),
      permissionCount: templateForm.permissionIds.length,
      expireTime: templateForm.expireTime || '',
      permissionIds: [...templateForm.permissionIds]
    };

    const index = templates.value.findIndex((item) => item.roleId === normalized.roleId);
    if (index >= 0) {
      templates.value[index] = normalized;
    } else {
      templates.value.unshift(normalized);
    }
    selectedTemplate.value = normalized;
    templateDialogVisible.value = false;
    ElMessage.success('模板已保存');
  } finally {
    submitting.value = false;
  }
}

function openAssignDialog(row?: TemplateRow) {
  assignRoleId.value = row?.roleId ?? templates.value[0]?.roleId ?? '';
  assignPermissionIds.value = row ? [...row.permissionIds] : [];
  assignDialogVisible.value = true;
}

async function submitAssign() {
  if (!ensure(assignRoleId.value, '请选择目标模板')) {
    return;
  }
  if (!ensure(assignPermissionIds.value.length, '请至少选择一项授权权限')) {
    return;
  }
  const targetName = templates.value.find((item) => item.roleId === assignRoleId.value)?.roleName ?? '当前模板';
  const confirmed = await confirmAction(`确认向“${targetName}”分配所选权限吗？`, '权限分配确认');
  if (!confirmed) {
    return;
  }
  submitting.value = true;
  try {
    await assignPermission({
      roleId: assignRoleId.value,
      permissionIds: assignPermissionIds.value
    });
    const target = templates.value.find((item) => item.roleId === assignRoleId.value);
    if (target) {
      target.permissionIds = [...assignPermissionIds.value];
      target.permissionCount = assignPermissionIds.value.length;
      selectedTemplate.value = target;
    }
    assignDialogVisible.value = false;
    ElMessage.success('权限已分配');
  } finally {
    submitting.value = false;
  }
}

applyFilter();
</script>

<style scoped>
.row-actions,.permission-list{display:flex;flex-wrap:wrap;gap:10px}
.toolbar-search{width:240px}
.template-cell p{margin:0;color:var(--text-secondary);line-height:1.7}
.template-cell strong{display:block;margin-bottom:6px}
.permission-list{margin-top:12px}
@media (max-width:768px){.toolbar-search{width:100%}}
</style>
