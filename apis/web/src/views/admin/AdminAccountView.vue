<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">管理员核心页</span>
        <h1 class="page-hero__title">账户管理</h1>
        <p class="page-hero__desc">
          负责账号创建、注册审核、冻结解冻与密码重置。教师不开放自助注册。
        </p>
      </div>
      <PageToolbar>
        <template #filters>
          <el-input v-model="keyword" class="toolbar-search" clearable placeholder="账号 / 姓名 / 联系方式" @keyup.enter="loadAccounts" />
          <el-select v-model="roleType" class="toolbar-select" clearable placeholder="角色筛选">
            <el-option label="学生" value="student" />
            <el-option label="教师" value="teacher" />
            <el-option label="管理员" value="admin" />
            <el-option label="超级管理员" value="super_admin" />
            <el-option label="审计员" value="auditor" />
            <el-option label="运维" value="ops" />
          </el-select>
        </template>
        <template #actions>
          <el-button @click="loadAccounts">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <AuthorizedAction permission="account.create" type="primary" @click="openCreateDialog">
            新建账号
          </AuthorizedAction>
        </template>
      </PageToolbar>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="账户列表" description="支持管理员创建账号、审核注册申请、冻结解冻账号以及密码重置。">
          <template #extra>
            <span class="soft-tag soft-tag--success">共 {{ total }} 条</span>
          </template>
        </SectionHeader>

        <StatePanel v-if="loadError" type="error" title="账户列表加载失败" :description="loadError" action-text="重新加载" @action="loadAccounts" />
        <el-table v-else :data="accounts" v-loading="loading" stripe empty-text="暂无账户">
          <el-table-column label="账号" min-width="130">
            <template #default="{ row }">
              <div class="account-cell">
                <strong>{{ row.username }}</strong>
                <p>{{ row.email || row.phone || '暂无联系方式' }}</p>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="姓名" width="120">
            <template #default="{ row }">{{ row.realName }}</template>
          </el-table-column>
          <el-table-column label="角色" width="120">
            <template #default="{ row }">
              <el-tag :type="roleTagType(row.roleType)" round effect="light">{{ roleLabel(row.roleType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="180">
            <template #default="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="240">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button text type="primary" @click="previewAccount(row)">详情</el-button>
                <AuthorizedAction text permission="account.audit" type="success" @click="audit(row)">
                  审核通过
                </AuthorizedAction>
                <AuthorizedAction text permission="account.freeze" type="warning" @click="toggleFreeze(row)">
                  冻结切换
                </AuthorizedAction>
                <AuthorizedAction text permission="account.resetPassword" type="danger" @click="openResetDialog(row)">
                  重置密码
                </AuthorizedAction>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <PaginationFooter :current-page="pageNum" :page-size="pageSize" :total="total" @change="handlePageChange" />
      </article>

      <article class="surface-section glass-card">
        <SectionHeader title="账户详情" description="当前视图聚焦基础身份信息与管理员操作说明。" />

        <InfoPreviewCard v-if="selectedAccount" :badge="roleLabel(selectedAccount.roleType)" :title="selectedAccount.realName" :lines="selectedAccountLines" />
        <StatePanel v-else type="empty" title="暂无账户详情" description="请选择一条账户记录查看详情。" />
      </article>
    </section>

    <el-dialog v-model="createDialogVisible" title="新建账号" width="700px">
      <el-form label-position="top">
        <FormGrid>
          <el-form-item label="账号">
            <el-input v-model="createForm.username" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="createForm.realName" />
          </el-form-item>
        </FormGrid>
        <FormGrid>
          <el-form-item label="角色">
            <el-select v-model="createForm.roleType">
              <el-option label="学生" value="student" />
              <el-option label="教师" value="teacher" />
              <el-option label="管理员" value="admin" />
              <el-option label="审计员" value="auditor" />
              <el-option label="运维" value="ops" />
            </el-select>
          </el-form-item>
          <el-form-item label="初始密码">
            <el-input v-model="createForm.password" show-password />
          </el-form-item>
        </FormGrid>
        <FormGrid>
          <el-form-item label="手机号">
            <el-input v-model="createForm.phone" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="createForm.email" />
          </el-form-item>
        </FormGrid>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="account.create" type="primary" :loading="submitting" @click="submitCreate">保存</AuthorizedAction>
      </template>
    </el-dialog>

    <el-dialog v-model="resetDialogVisible" title="重置密码" width="480px">
      <el-form label-position="top">
        <el-form-item label="新密码">
          <el-input v-model="resetPasswordValue" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <AuthorizedAction permission="account.resetPassword" type="primary" :loading="submitting" @click="submitReset">确认重置</AuthorizedAction>
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
import StatePanel from '@/components/StatePanel.vue';
import { useActionGuard } from '@/composables/useActionGuard';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { queryAccounts, createAccount, auditAccount, freezeAccount, resetPassword } from '@/api/account';

type AccountRow = Record<string, any>;

const submitting = ref(false);
const keyword = ref('');
const roleType = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const accounts = ref<AccountRow[]>([]);
const selectedAccount = ref<AccountRow | null>(null);
const createDialogVisible = ref(false);
const resetDialogVisible = ref(false);
const currentAccountId = ref('');
const freezeMap = reactive<Record<string, boolean>>({});
const { ensure, confirmAction } = useActionGuard();
const { loading, loadError, runLoad } = useLoadFeedback();
const createForm = reactive({
  username: '',
  realName: '',
  roleType: 'teacher',
  password: 'Exam@123',
  phone: '',
  email: ''
});
const resetPasswordValue = ref('Exam@123456');

const metrics = computed(() => [
  { label: '账户总数', value: total.value },
  { label: '教师账号', value: accounts.value.filter((item) => item.roleType === 'teacher').length },
  { label: '冻结标记', value: Object.values(freezeMap).filter(Boolean).length }
]);
const selectedAccountLines = computed(() => {
  if (!selectedAccount.value) {
    return [];
  }
  return [
    `登录账号：${selectedAccount.value.username}`,
    `手机号码：${selectedAccount.value.phone || '-'}`,
    `邮箱地址：${selectedAccount.value.email || '-'}`,
    '操作提醒：学生自助注册后需管理员审核，教师账号建议由管理员直接创建。'
  ];
});

function roleLabel(value: string) {
  return value === 'student'
    ? '学生'
    : value === 'teacher'
      ? '教师'
      : value === 'admin'
        ? '管理员'
        : value === 'super_admin'
          ? '超级管理员'
          : value === 'auditor'
            ? '审计员'
            : '运维';
}

function roleTagType(value: string) {
  return value === 'teacher'
    ? 'success'
    : value === 'admin' || value === 'super_admin'
      ? 'danger'
      : value === 'auditor'
        ? 'warning'
        : 'info';
}

function resetCreateForm() {
  createForm.username = '';
  createForm.realName = '';
  createForm.roleType = 'teacher';
  createForm.password = 'Exam@123';
  createForm.phone = '';
  createForm.email = '';
}

async function loadAccounts() {
  await runLoad(async () => {
    const result = await queryAccounts({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      roleType: roleType.value || undefined
    });
    accounts.value = result.list;
    total.value = result.total;
    if (!accounts.value.length) {
      selectedAccount.value = null;
    } else if (!selectedAccount.value || !accounts.value.some((item) => item.accountId === selectedAccount.value?.accountId)) {
      selectedAccount.value = accounts.value[0];
    }
  }, '账户列表加载失败，请稍后重试');
}

function handlePageChange(value: number) {
  pageNum.value = value;
  loadAccounts();
}

function resetFilters() {
  keyword.value = '';
  roleType.value = '';
  pageNum.value = 1;
  loadAccounts();
}

function previewAccount(row: AccountRow) {
  selectedAccount.value = row;
}

function openCreateDialog() {
  resetCreateForm();
  createDialogVisible.value = true;
}

async function submitCreate() {
  const username = createForm.username.trim();
  const realName = createForm.realName.trim();
  if (!ensure(username, '请输入账号')) {
    return;
  }
  if (!ensure(realName, '请输入姓名')) {
    return;
  }
  if (!ensure(createForm.password.trim(), '请输入初始密码')) {
    return;
  }
  submitting.value = true;
  try {
    await createAccount({ ...createForm, username, realName });
    ElMessage.success('账号已创建');
    createDialogVisible.value = false;
    await loadAccounts();
  } finally {
    submitting.value = false;
  }
}

async function audit(row: AccountRow) {
  const confirmed = await confirmAction(`确认审核通过账号“${row.username}”吗？`, '审核确认');
  if (!confirmed) {
    return;
  }
  await auditAccount({
    accountId: row.accountId,
    auditResult: 'approve'
  });
  ElMessage.success('审核已通过');
  await loadAccounts();
}

async function toggleFreeze(row: AccountRow) {
  const next = !freezeMap[row.accountId];
  const confirmed = await confirmAction(
    next ? `确认冻结账号“${row.username}”吗？` : `确认解冻账号“${row.username}”吗？`,
    next ? '冻结账号确认' : '解冻账号确认'
  );
  if (!confirmed) {
    return;
  }
  await freezeAccount({
    accountId: row.accountId,
    isFrozen: next
  });
  freezeMap[row.accountId] = next;
  ElMessage.success(next ? '账号已冻结' : '账号已解冻');
}

function openResetDialog(row: AccountRow) {
  currentAccountId.value = row.accountId;
  resetPasswordValue.value = 'Exam@123456';
  resetDialogVisible.value = true;
}

async function submitReset() {
  const password = resetPasswordValue.value.trim();
  if (!ensure(currentAccountId.value, '未找到要重置密码的账号')) {
    return;
  }
  if (!ensure(password.length >= 8, '重置密码至少需要 8 位')) {
    return;
  }
  const confirmed = await confirmAction('确认重置该账号密码吗？', '重置密码确认');
  if (!confirmed) {
    return;
  }
  submitting.value = true;
  try {
    await resetPassword({
      accountId: currentAccountId.value,
      newPassword: password
    });
    resetDialogVisible.value = false;
    ElMessage.success('密码已重置');
  } finally {
    submitting.value = false;
  }
}

onMounted(loadAccounts);
</script>

<style scoped>
.row-actions{display:flex;flex-wrap:wrap;gap:10px}
.toolbar-search{width:240px}
.toolbar-select{width:150px}
.account-cell p{margin:0;color:var(--text-secondary);line-height:1.7}
.account-cell strong{display:block;margin-bottom:6px}
@media (max-width:768px){.toolbar-search,.toolbar-select{width:100%}}
</style>
