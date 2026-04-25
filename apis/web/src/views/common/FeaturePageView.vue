<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">{{ page.scene }}</span>
        <h1 class="page-hero__title">{{ page.title }}</h1>
        <p class="page-hero__desc">{{ page.desc }}</p>
      </div>
      <div class="hero-actions">
        <el-input v-model="keyword" class="hero-actions__search" clearable placeholder="关键字检索" @keyup.enter="reload" />
        <el-button @click="reload">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button v-for="action in toolbarActions" :key="action.key" :type="action.type" @click="runToolbarAction(action.key)">
          {{ action.label }}
        </el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card glass-card">
        <p class="metric-card__label">{{ metric.label }}</p>
        <p class="metric-card__value">{{ metric.value }}</p>
      </article>
    </section>

    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <div class="section-head">
          <div>
            <h3>{{ page.tableTitle }}</h3>
            <p>{{ page.tableDesc }}</p>
          </div>
          <span class="soft-tag soft-tag--success">共 {{ total }} 条</span>
        </div>

        <el-table :data="records" v-loading="loading" stripe empty-text="暂无数据">
          <el-table-column v-for="column in page.columns" :key="column.key" :label="column.label" :min-width="column.minWidth" :width="column.width">
            <template #default="{ row }">
              <el-tag v-if="column.tag" :type="tagType(row[column.key])" effect="light" round>{{ formatCell(column.key, row[column.key]) }}</el-tag>
              <span v-else>{{ formatCell(column.key, row[column.key]) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="rowActions.length" label="操作" fixed="right" min-width="220">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button v-for="action in rowActions" :key="action.key" :type="action.type" text @click="runRowAction(action.key, row)">
                  {{ rowLabel(action.key, row) }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          <el-pagination background layout="total, prev, pager, next" :current-page="pageNum" :page-size="pageSize" :total="total" @current-change="changePage" />
        </div>
      </article>

      <article class="surface-section glass-card">
        <div class="section-head">
          <div>
            <h3>{{ asideTitle }}</h3>
            <p>{{ asideDesc }}</p>
          </div>
        </div>

        <div v-if="timeline.length" class="timeline-list">
          <div v-for="item in timeline" :key="`${item.time}-${item.title}`" class="timeline-item">
            <span class="soft-tag">{{ item.time }}</span>
            <h4>{{ item.title }}</h4>
            <p>{{ item.desc }}</p>
          </div>
        </div>
        <div v-else-if="analysisCards.length" class="analysis-grid">
          <div v-for="item in analysisCards" :key="item.label" class="analysis-card">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
        <div v-else-if="assistTags.length" class="assist-tags">
          <el-tag v-for="tag in assistTags" :key="tag" round effect="plain">{{ tag }}</el-tag>
        </div>
        <div v-else class="empty-tip">{{ page.hint }}</div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { queryAccounts, createAccount, auditAccount, freezeAccount, resetPassword } from '@/api/account';
import { queryClasses, createClass, updateClass, deleteClass, applyJoinClass, approveJoinClass, removeStudent, quitClass, importClasses } from '@/api/class';
import { queryQuestions, queryCategories, createQuestion, updateQuestion, toggleQuestionStatus, importQuestions, exportQuestions, auditQuestion } from '@/api/question';
import { queryPapers, createManualPaper, createAutoPaper, publishPaper, terminatePaper, recyclePaper, exportPaper } from '@/api/paper';
import { queryExams, createExam, updateExamParams, distributeExam, toggleExamStatus, approveRetest } from '@/api/examCore';
import { queryScores, queryScoreDetail, applyScoreRecheck, manualScore, publishScores, analyzeScores, exportScores, handleAppeal } from '@/api/score';
import { queryIssues, createIssue, handleIssue, transferIssue, closeIssue, trackIssue } from '@/api/issue';
import { queryConfigs, updateConfig, queryAlarms, updateAlarmSetting, queryLogs, exportLogs, queryBackups, runBackup, restoreBackup, saveRoleTemplate, assignPermission } from '@/api/system';
import { downloadFile } from '@/api/resource';

type RowData = Record<string, any>;
type Column = { key: string; label: string; minWidth?: number; width?: number; tag?: boolean };
type Action = { key: string; label: string; type?: 'primary' | 'success' | 'warning' | 'danger' | 'default' };

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const records = ref<RowData[]>([]);
const assistTags = ref<string[]>([]);
const timeline = ref<Array<{ time: string; title: string; desc: string }>>([]);
const analysis = ref<RowData | null>(null);

const columns: Record<string, Column[]> = {
  class: [{ key: 'className', label: '班级名称', minWidth: 140 }, { key: 'teacherName', label: '教师', width: 110 }, { key: 'classCode', label: '班级码', width: 110 }, { key: 'approvedMemberCount', label: '正式人数', width: 100 }, { key: 'pendingMemberCount', label: '待审核', width: 100 }],
  exam: [{ key: 'examName', label: '考试名称', minWidth: 160 }, { key: 'classNames', label: '班级', minWidth: 150 }, { key: 'startTime', label: '开始时间', minWidth: 170 }, { key: 'duration', label: '时长', width: 90 }, { key: 'status', label: '状态', width: 100, tag: true }],
  score: [{ key: 'examName', label: '考试名称', minWidth: 150 }, { key: 'studentName', label: '学生', width: 110 }, { key: 'totalScore', label: '总分', width: 80 }, { key: 'publishStatus', label: '发布状态', width: 110, tag: true }, { key: 'recheckStatus', label: '复核状态', width: 110, tag: true }],
  issue: [{ key: 'title', label: '标题', minWidth: 180 }, { key: 'type', label: '类型', width: 100, tag: true }, { key: 'handlerName', label: '处理人', width: 110 }, { key: 'status', label: '状态', width: 100, tag: true }],
  question: [{ key: 'content', label: '题目内容', minWidth: 220 }, { key: 'categoryName', label: '分类', width: 110 }, { key: 'type', label: '题型', width: 100, tag: true }, { key: 'auditStatus', label: '审核', width: 100, tag: true }],
  paper: [{ key: 'paperName', label: '试卷名称', minWidth: 160 }, { key: 'examTime', label: '时长', width: 90 }, { key: 'passScore', label: '及格线', width: 90 }, { key: 'status', label: '状态', width: 110, tag: true }, { key: 'publishScope', label: '发布范围', minWidth: 150 }],
  account: [{ key: 'username', label: '账号', width: 120 }, { key: 'realName', label: '姓名', width: 110 }, { key: 'roleType', label: '角色', width: 110, tag: true }, { key: 'phone', label: '手机号', minWidth: 130 }, { key: 'email', label: '邮箱', minWidth: 180 }],
  config: [{ key: 'configKey', label: '配置键', minWidth: 220 }, { key: 'category', label: '分类', width: 110, tag: true }, { key: 'configValue', label: '当前值', width: 110 }, { key: 'desc', label: '说明', minWidth: 180 }],
  log: [{ key: 'operator', label: '操作人', width: 120 }, { key: 'action', label: '操作内容', minWidth: 180 }, { key: 'result', label: '结果', width: 100, tag: true }, { key: 'time', label: '时间', minWidth: 170 }],
  alarm: [{ key: 'alarmType', label: '告警类型', minWidth: 150 }, { key: 'level', label: '级别', width: 100, tag: true }, { key: 'status', label: '状态', width: 100, tag: true }, { key: 'threshold', label: '阈值', width: 110 }, { key: 'createTime', label: '时间', minWidth: 170 }],
  backup: [{ key: 'backupId', label: '备份编号', minWidth: 120 }, { key: 'backupType', label: '类型', width: 100, tag: true }, { key: 'status', label: '状态', width: 100, tag: true }, { key: 'lifecycleStage', label: '生命周期', minWidth: 120 }, { key: 'updateTime', label: '更新时间', minWidth: 170 }],
  role: [{ key: 'roleName', label: '模板名称', minWidth: 160 }, { key: 'permissionCount', label: '权限数', width: 100 }, { key: 'expireTime', label: '失效时间', minWidth: 150 }]
};

const pages: Record<string, { scene: string; title: string; desc: string; tableTitle: string; tableDesc: string; hint: string; columns: Column[] }> = {
  '/student/class': { scene: '学生端', title: '我的班级', desc: '支持班级码入班与退出非强制班级。', tableTitle: '班级列表', tableDesc: '学生仅可看到自己的班级。', hint: '可通过班级码申请入班。', columns: columns.class },
  '/teacher/class': { scene: '教师端', title: '班级管理', desc: '已补齐教师创建班级能力。', tableTitle: '班级列表', tableDesc: '教师默认最多创建 1 个专属班级。', hint: '支持审批入班与移除学生。', columns: columns.class },
  '/admin/class': { scene: '管理员端', title: '班级管理', desc: '支持班级台账统一维护。', tableTitle: '班级台账', tableDesc: '适合统一导入和批量维护。', hint: '支持导入、编辑和删除。', columns: columns.class },
  '/student/exam': { scene: '学生端', title: '我的考试', desc: '进入考试后会触发倒计时和自动保存。', tableTitle: '考试列表', tableDesc: '展示当前可参加考试。', hint: '点击进入考试可跳转答题页。', columns: columns.exam },
  '/teacher/exam': { scene: '教师端', title: '考试管理', desc: '已补齐考试暂停/恢复入口。', tableTitle: '考试列表', tableDesc: '支持创建、分发、暂停和补考审批。', hint: '可直接对考试执行暂停或恢复。', columns: columns.exam },
  '/student/score': { scene: '学生端', title: '成绩查询', desc: '支持查看成绩与申请复核。', tableTitle: '成绩列表', tableDesc: '主观题复核按流程提交。', hint: '复核申请会更新到问题流转中。', columns: columns.score },
  '/teacher/score': { scene: '教师端', title: '成绩管理', desc: '已补齐成绩发布能力。', tableTitle: '成绩列表', tableDesc: '支持人工阅卷与导出。', hint: '右侧会展示成绩分析。', columns: columns.score },
  '/auditor/score-audit': { scene: '审计员端', title: '成绩核查', desc: '审计员全程只读。', tableTitle: '成绩核查', tableDesc: '只查看，不写入。', hint: '可查看成绩明细。', columns: columns.score },
  '/student/issue': { scene: '学生端', title: '问题申报', desc: '学生可提交考试类问题。', tableTitle: '问题单列表', tableDesc: '默认流转给教师处理。', hint: '点击跟踪可查看处理时间线。', columns: columns.issue },
  '/teacher/issue': { scene: '教师端', title: '问题处理', desc: '支持接单、转派、关闭。', tableTitle: '问题单列表', tableDesc: '聚焦考试类问题。', hint: '已补齐问题关闭入口。', columns: columns.issue },
  '/admin/issue': { scene: '管理员端', title: '业务问题', desc: '负责业务类问题流转。', tableTitle: '业务问题单', tableDesc: '普通管理员只处理业务问题。', hint: '可查看完整处理过程。', columns: columns.issue },
  '/auditor/issue': { scene: '审计员端', title: '问题跟踪', desc: '审计员仅只读跟踪。', tableTitle: '问题跟踪列表', tableDesc: '不执行写操作。', hint: '可查看完整时间线。', columns: columns.issue },
  '/ops/issue': { scene: '运维端', title: '系统类问题', desc: '运维负责系统问题处理。', tableTitle: '系统问题单', tableDesc: '可接单、转派、关闭。', hint: '适用于系统类问题闭环。', columns: columns.issue },
  '/teacher/question': { scene: '教师端', title: '试题管理', desc: '已补齐试题与分类查询接口。', tableTitle: '试题列表', tableDesc: '支持增改、停用、导入导出。', hint: '右侧展示分类清单。', columns: columns.question },
  '/admin/question-audit': { scene: '管理员端', title: '试题审核', desc: '统一审核教师提交的试题。', tableTitle: '待审试题', tableDesc: '支持审核通过与导出。', hint: '仅展示待审核试题。', columns: columns.question },
  '/teacher/paper': { scene: '教师端', title: '试卷管理', desc: '已补齐试卷回收与导出接口。', tableTitle: '试卷列表', tableDesc: '支持手动组卷和自动组卷。', hint: '支持发布、终止、回收和导出。', columns: columns.paper },
  '/admin/account': { scene: '管理员端', title: '账户管理', desc: '学生自助注册后等待管理员审核。', tableTitle: '账户列表', tableDesc: '支持创建、审核、冻结、重置密码。', hint: '教师不开放自助注册。', columns: columns.account },
  '/admin/system-config': { scene: '管理员端', title: '系统配置', desc: '维护考试与安全类配置。', tableTitle: '配置项列表', tableDesc: '可更新配置值和告警阈值。', hint: '支持更新基础告警阈值。', columns: columns.config },
  '/admin/log': { scene: '管理员端', title: '业务日志', desc: '普通管理员仅可查看业务日志。', tableTitle: '业务日志列表', tableDesc: '不提供审计导出。', hint: '只展示业务日志。', columns: columns.log },
  '/super-admin/log': { scene: '超级管理员端', title: '日志审计', desc: '支持全量日志查看与导出。', tableTitle: '全量日志', tableDesc: '覆盖业务、系统、审计日志。', hint: '支持日志导出。', columns: columns.log },
  '/auditor/log': { scene: '审计员端', title: '日志审计', desc: '审计员拥有只读日志视图。', tableTitle: '审计日志', tableDesc: '支持导出归档。', hint: '默认只看审计日志。', columns: columns.log },
  '/ops/log': { scene: '运维端', title: '系统日志', desc: '查询系统运行与备份日志。', tableTitle: '系统日志', tableDesc: '运维只看系统日志。', hint: '聚焦运行链路。', columns: columns.log },
  '/auditor/alarm': { scene: '审计员端', title: '异常行为监控', desc: '审计员只读查看异常告警。', tableTitle: '告警列表', tableDesc: '不参与处置。', hint: '适合审计核查。', columns: columns.alarm },
  '/ops/alarm': { scene: '运维端', title: '系统告警', desc: '当前仅实现告警查询展示。', tableTitle: '系统告警列表', tableDesc: '不实现确认处理状态。', hint: '等待后端补齐处置接口。', columns: columns.alarm },
  '/super-admin/data-security': { scene: '超级管理员端', title: '数据安全中心', desc: '恢复数据需双验证码。', tableTitle: '备份记录', tableDesc: '支持恢复数据。', hint: 'Mock 验证码为 9527 与 3141。', columns: columns.backup },
  '/ops/data-security': { scene: '运维端', title: '数据安全中心', desc: '运维只能备份不能恢复。', tableTitle: '备份记录', tableDesc: '支持查询和发起备份。', hint: '运维侧不开放恢复入口。', columns: columns.backup },
  '/super-admin/role': { scene: '超级管理员端', title: '角色权限管理', desc: '维护权限模板并执行授权。', tableTitle: '角色模板', tableDesc: '以模板能力演示为主。', hint: '可新增模板并分配权限。', columns: columns.role }
};

const toolbarActionMap: Record<string, Action[]> = {
  '/student/class': [{ key: 'applyJoin', label: '申请入班', type: 'primary' }],
  '/student/issue': [{ key: 'createIssue', label: '新建问题', type: 'primary' }],
  '/teacher/class': [{ key: 'createClass', label: '创建班级', type: 'primary' }],
  '/admin/class': [{ key: 'createClass', label: '新建班级', type: 'primary' }, { key: 'importClass', label: '导入班级', type: 'success' }],
  '/teacher/question': [{ key: 'createQuestion', label: '新增试题', type: 'primary' }, { key: 'importQuestion', label: '批量导入' }, { key: 'exportQuestion', label: '导出题库', type: 'success' }],
  '/teacher/paper': [{ key: 'createManualPaper', label: '手动组卷', type: 'primary' }, { key: 'createAutoPaper', label: '自动组卷', type: 'success' }],
  '/teacher/exam': [{ key: 'createExam', label: '新建考试', type: 'primary' }],
  '/admin/account': [{ key: 'createAccount', label: '新建账号', type: 'primary' }],
  '/admin/system-config': [{ key: 'updateAlarm', label: '更新告警阈值', type: 'warning' }],
  '/super-admin/log': [{ key: 'exportLog', label: '导出日志', type: 'success' }],
  '/auditor/log': [{ key: 'exportLog', label: '导出日志', type: 'success' }],
  '/super-admin/data-security': [{ key: 'restoreData', label: '恢复数据', type: 'danger' }],
  '/ops/data-security': [{ key: 'backupData', label: '发起备份', type: 'primary' }],
  '/super-admin/role': [{ key: 'saveRole', label: '新增模板', type: 'primary' }, { key: 'assignRolePermission', label: '分配权限', type: 'warning' }]
};

const rowActionMap: Record<string, Action[]> = {
  '/student/class': [{ key: 'quitClass', label: '退出班级', type: 'danger' }],
  '/student/exam': [{ key: 'enterExam', label: '进入考试', type: 'primary' }],
  '/student/score': [{ key: 'scoreDetail', label: '查看明细' }, { key: 'applyRecheck', label: '申请复核', type: 'warning' }],
  '/teacher/class': [{ key: 'approveJoin', label: '审批入班', type: 'success' }, { key: 'removeStudent', label: '移除学生', type: 'danger' }],
  '/teacher/question': [{ key: 'updateQuestion', label: '编辑题目' }, { key: 'toggleQuestion', label: '停用切换', type: 'warning' }],
  '/teacher/paper': [{ key: 'publishPaper', label: '发布试卷', type: 'primary' }, { key: 'terminatePaper', label: '终止', type: 'danger' }, { key: 'recyclePaper', label: '回收', type: 'warning' }, { key: 'exportPaper', label: '导出', type: 'success' }],
  '/teacher/exam': [{ key: 'updateExam', label: '修改时长' }, { key: 'distributeExam', label: '分发考试', type: 'primary' }, { key: 'toggleExam', label: '暂停/恢复', type: 'warning' }, { key: 'approveRetest', label: '补考审批', type: 'success' }],
  '/teacher/score': [{ key: 'manualScore', label: '人工阅卷' }, { key: 'publishScore', label: '发布成绩', type: 'primary' }, { key: 'exportScore', label: '导出成绩', type: 'success' }, { key: 'handleAppeal', label: '处理复核', type: 'warning' }],
  '/student/issue': [{ key: 'trackIssue', label: '查看进度' }],
  '/teacher/issue': [{ key: 'handleIssue', label: '接单', type: 'primary' }, { key: 'transferIssue', label: '转派', type: 'warning' }, { key: 'closeIssue', label: '关闭', type: 'danger' }, { key: 'trackIssue', label: '跟踪' }],
  '/admin/issue': [{ key: 'handleIssue', label: '接单', type: 'primary' }, { key: 'transferIssue', label: '转派', type: 'warning' }, { key: 'closeIssue', label: '关闭', type: 'danger' }, { key: 'trackIssue', label: '跟踪' }],
  '/ops/issue': [{ key: 'handleIssue', label: '接单', type: 'primary' }, { key: 'transferIssue', label: '转派', type: 'warning' }, { key: 'closeIssue', label: '关闭', type: 'danger' }, { key: 'trackIssue', label: '跟踪' }],
  '/auditor/issue': [{ key: 'trackIssue', label: '查看进度' }],
  '/admin/account': [{ key: 'auditAccount', label: '审核', type: 'success' }, { key: 'freezeAccount', label: '冻结', type: 'danger' }, { key: 'resetPassword', label: '重置密码', type: 'warning' }],
  '/admin/class': [{ key: 'updateClass', label: '编辑' }, { key: 'deleteClass', label: '删除', type: 'danger' }],
  '/admin/question-audit': [{ key: 'auditQuestion', label: '审核通过', type: 'success' }],
  '/admin/system-config': [{ key: 'updateConfig', label: '修改配置', type: 'warning' }],
  '/super-admin/data-security': [{ key: 'restoreData', label: '恢复此备份', type: 'danger' }],
  '/auditor/score-audit': [{ key: 'scoreDetail', label: '查看明细' }]
};

const page = computed(() => pages[route.path]);
const metrics = computed(() => [{ label: '当前页数据', value: records.value.length }, { label: '总记录数', value: total.value }, { label: '当前页码', value: pageNum.value }]);
const toolbarActions = computed<Action[]>(() => toolbarActionMap[route.path] ?? []);
const rowActions = computed<Action[]>(() => rowActionMap[route.path] ?? []);
const asideTitle = computed(() => route.path.includes('/issue') ? '处理时间线' : route.path === '/teacher/score' ? '成绩分析' : '辅助信息');
const asideDesc = computed(() => route.path.includes('/issue') ? '选择问题单后展示流转过程。' : route.path === '/teacher/score' ? '基于当前成绩列表生成概览。' : '展示分类、恢复提示或只读说明。');
const analysisCards = computed(() => analysis.value ? [{ label: '平均分', value: analysis.value.average ?? '-' }, { label: '及格率', value: `${analysis.value.passRate ?? '-'}%` }, { label: '分段数量', value: Array.isArray(analysis.value.distribution) ? analysis.value.distribution.length : 0 }] : []);

function formatCell(key: string, value: unknown) { if (key === 'publishScope' || key === 'classNames') return Array.isArray(value) ? value.join('、') || '-' : '-'; if (key === 'recheckStatus' && !value) return '无'; if (value === undefined || value === null || value === '') return '-'; return typeof value === 'boolean' ? (value ? '是' : '否') : String(value); }
function tagType(value: unknown) { const text = String(value ?? ''); if (['published', 'approved', 'success', 'closed', 'SUCCESS'].includes(text)) return 'success'; if (['pending', 'processing', 'draft', 'RUNNING', 'medium'].includes(text)) return 'warning'; if (['rejected', 'terminated', 'recycled', 'FAILED', 'high'].includes(text)) return 'danger'; return 'info'; }
function rowLabel(key: string, row: RowData) { return key === 'toggleExam' ? (row.isPaused ? '恢复考试' : '暂停考试') : key === 'toggleQuestion' ? (row.isDisabled ? '启用题目' : '停用题目') : rowActions.value.find((item) => item.key === key)?.label ?? key; }
async function promptText(title: string, inputValue = '') { const result = await ElMessageBox.prompt('请输入内容', title, { confirmButtonText: '确认', cancelButtonText: '取消', inputValue }); return result.value; }
async function exportResult(result: RowData) { const download = await downloadFile({ fileKey: result.fileKey }); ElMessage.success(`导出任务已生成：${download.fileKey}`); }
async function restoreAction(row?: RowData) { const verifyCode1 = await promptText('第一位授权验证码', '9527'); const verifyCode2 = await promptText('第二位授权验证码', '3141'); await restoreBackup({ backupId: row?.backupId ?? 'b-1', verifyCode1, verifyCode2 }); }

async function loadPage() {
  loading.value = true; assistTags.value = []; timeline.value = []; analysis.value = null;
  try {
    if (route.path === '/super-admin/role') { records.value = [{ roleName: '教师标准权限', permissionCount: 18, expireTime: '-' }, { roleName: '审计临时核查权限', permissionCount: 9, expireTime: '7 天后失效' }].filter((item) => item.roleName.includes(keyword.value)); total.value = records.value.length; return; }
    if (['/student/class', '/teacher/class', '/admin/class'].includes(route.path)) { const result = await queryClasses({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }); records.value = result.list; total.value = result.total; return; }
    if (['/student/exam', '/teacher/exam'].includes(route.path)) { const result = await queryExams({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; return; }
    if (['/student/score', '/teacher/score', '/auditor/score-audit'].includes(route.path)) { const result = await queryScores({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; if (route.path === '/teacher/score') analysis.value = await analyzeScores({}); return; }
    if (['/student/issue', '/teacher/issue', '/admin/issue', '/auditor/issue', '/ops/issue'].includes(route.path)) { const result = await queryIssues({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; return; }
    if (['/teacher/question', '/admin/question-audit'].includes(route.path)) { const [questionResult, categoryResult] = await Promise.all([queryQuestions({ pageNum: pageNum.value, pageSize: pageSize.value, auditStatus: route.path === '/admin/question-audit' ? 'pending' : undefined }), queryCategories({ pageNum: 1, pageSize: 20 })]); records.value = questionResult.list; total.value = questionResult.total; assistTags.value = Array.isArray(categoryResult.list) ? categoryResult.list.map((item: RowData) => String(item.name ?? '未分类')) : []; return; }
    if (route.path === '/teacher/paper') { const result = await queryPapers({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; return; }
    if (route.path === '/admin/account') { const result = await queryAccounts({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }); records.value = result.list; total.value = result.total; return; }
    if (route.path === '/admin/system-config') { const result = await queryConfigs({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; return; }
    if (route.path === '/admin/log') { const result = await queryLogs({ pageNum: pageNum.value, pageSize: pageSize.value, logType: 'business' }); records.value = result.list; total.value = result.total; return; }
    if (route.path === '/super-admin/log') { const result = await queryLogs({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; return; }
    if (route.path === '/auditor/log') { const result = await queryLogs({ pageNum: pageNum.value, pageSize: pageSize.value, logType: 'audit' }); records.value = result.list; total.value = result.total; return; }
    if (route.path === '/ops/log') { const result = await queryLogs({ pageNum: pageNum.value, pageSize: pageSize.value, logType: 'system' }); records.value = result.list; total.value = result.total; return; }
    if (['/auditor/alarm', '/ops/alarm'].includes(route.path)) { const result = await queryAlarms({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; return; }
    if (['/super-admin/data-security', '/ops/data-security'].includes(route.path)) { const result = await queryBackups({ pageNum: pageNum.value, pageSize: pageSize.value }); records.value = result.list; total.value = result.total; }
  } finally { loading.value = false; }
}

async function reload() { pageNum.value = 1; await loadPage(); }
async function resetSearch() { keyword.value = ''; await reload(); }
async function changePage(value: number) { pageNum.value = value; await loadPage(); }

async function runToolbarAction(key: string) {
  if (key === 'applyJoin') await applyJoinClass({ classCode: await promptText('申请加入班级', 'JAVA101') });
  if (key === 'createIssue') await createIssue({ type: 'exam', title: await promptText('新建问题单', '考试页面异常'), desc: '来自前端页面快速创建' });
  if (key === 'createClass') await createClass({ className: await promptText('创建班级', '前端新建班级'), description: '由前端页面创建' });
  if (key === 'importClass') await importClasses({ batchName: '管理员导入班级' });
  if (key === 'createQuestion') await createQuestion({ content: await promptText('新增试题', '请填写新的试题内容'), type: 'single', categoryId: 'qc-1', answer: '示例答案', analysis: '示例解析' });
  if (key === 'importQuestion') await importQuestions({ batchName: '教师批量导入' });
  if (key === 'exportQuestion') await exportResult(await exportQuestions({ scope: route.path }));
  if (key === 'createManualPaper') await createManualPaper({ paperName: await promptText('手动组卷', '前端手动组卷'), examTime: 90, passScore: 60, questionIds: ['q-1', 'q-3'] });
  if (key === 'createAutoPaper') await createAutoPaper({ paperName: await promptText('自动组卷', '前端自动组卷') });
  if (key === 'createExam') await createExam({ examName: await promptText('新建考试', '前端新建考试'), paperId: 'p-1', classIds: ['c-101'], duration: 90, startTime: new Date().toISOString() });
  if (key === 'createAccount') await createAccount({ username: await promptText('新建账号', 'teacher-auto'), password: 'Exam@123', roleType: 'teacher', realName: '新教师' });
  if (key === 'updateAlarm') await updateAlarmSetting({ alarmType: 'screen-out', threshold: await promptText('更新告警阈值', '3 次切屏'), recipients: ['顾清和'] });
  if (key === 'exportLog') await exportResult(await exportLogs({ scope: route.path }));
  if (key === 'backupData') await runBackup({ backupType: 'incremental' });
  if (key === 'restoreData') await restoreAction(records.value[0]);
  if (key === 'saveRole') await saveRoleTemplate({ roleName: await promptText('新增权限模板', '临时督导权限'), permissionIds: ['system.log.export', 'score.query'] });
  if (key === 'assignRolePermission') await assignPermission({ roleId: 'role-1', permissionIds: ['system.log.export', 'system.permission.assign'] });
  ElMessage.success('操作已完成'); await loadPage();
}

async function runRowAction(key: string, row: RowData) {
  if (key === 'quitClass') await quitClass({ classId: row.classId });
  if (key === 'enterExam') return router.push(`/student/exam/answer?examId=${row.examId}`);
  if (key === 'scoreDetail') { const result = await queryScoreDetail({ examId: row.examId, studentId: row.studentId ?? authStore.user?.accountId }); const detailList = Array.isArray(result.detail) ? result.detail : []; timeline.value = detailList.map((item: RowData) => ({ time: '评分明细', title: String(item.stem ?? '题目'), desc: `得分：${item.score ?? '-'} ${item.comment ?? ''}` })); return; }
  if (key === 'applyRecheck') await applyScoreRecheck({ examId: row.examId, reason: '申请主观题复核' });
  if (key === 'approveJoin') await approveJoinClass({ classId: row.classId, studentIds: row.pendingStudents?.length ? [row.pendingStudents[0]] : [] });
  if (key === 'removeStudent') await removeStudent({ classId: row.classId, studentId: row.students?.[0] ?? authStore.user?.accountId });
  if (key === 'updateQuestion') await updateQuestion({ questionId: row.questionId, content: await promptText('编辑题目', row.content), analysis: '前端更新后的解析', answer: row.answer ?? '示例答案' });
  if (key === 'toggleQuestion') await toggleQuestionStatus({ questionId: row.questionId, isDisabled: !row.isDisabled });
  if (key === 'publishPaper') await publishPaper({ paperId: row.paperId, classIds: ['c-101'] });
  if (key === 'terminatePaper') await terminatePaper({ paperId: row.paperId });
  if (key === 'recyclePaper') await recyclePaper({ paperId: row.paperId });
  if (key === 'exportPaper') await exportResult(await exportPaper({ paperId: row.paperId }));
  if (key === 'updateExam') await updateExamParams({ examId: row.examId, duration: Number(await promptText('修改考试时长', String(row.duration ?? 90))) });
  if (key === 'distributeExam') await distributeExam({ examId: row.examId });
  if (key === 'toggleExam') await toggleExamStatus({ examId: row.examId, isPaused: !row.isPaused });
  if (key === 'approveRetest') await approveRetest({ examId: row.examId, studentId: 'u-student-01' });
  if (key === 'manualScore') await manualScore({ examId: row.examId, studentId: row.studentId, score: Number(await promptText('录入主观题得分', String(row.subjectiveScore ?? 40))) });
  if (key === 'publishScore') await publishScores({ examId: row.examId });
  if (key === 'exportScore') await exportResult(await exportScores({ examId: row.examId }));
  if (key === 'handleAppeal') await handleAppeal({ appealId: row.scoreId });
  if (key === 'handleIssue') await handleIssue({ issueId: row.issueId, result: '已由当前角色接单处理' });
  if (key === 'transferIssue') await transferIssue({ issueId: row.issueId, toHandlerId: 'u-ops-01', reason: '前端演示转派' });
  if (key === 'closeIssue') await closeIssue({ issueId: row.issueId, comment: '页面端关闭问题单' });
  if (key === 'trackIssue') { const trackList = await trackIssue({ issueId: row.issueId }); timeline.value = Array.isArray(trackList) ? trackList.map((item: RowData) => ({ time: String(item.time ?? '-'), title: String(item.title ?? '流转记录'), desc: String(item.desc ?? '-') })) : []; return; }
  if (key === 'auditAccount') await auditAccount({ accountId: row.accountId, auditResult: 'approve' });
  if (key === 'freezeAccount') await freezeAccount({ accountId: row.accountId, isFrozen: true });
  if (key === 'resetPassword') await resetPassword({ accountId: row.accountId, newPassword: 'Exam@123456' });
  if (key === 'updateClass') await updateClass({ classId: row.classId, className: await promptText('修改班级名称', row.className), description: row.description ?? '已由前端更新' });
  if (key === 'deleteClass') await deleteClass({ classId: row.classId });
  if (key === 'auditQuestion') await auditQuestion({ questionId: row.questionId, auditResult: 'approve' });
  if (key === 'updateConfig') await updateConfig({ configKey: row.configKey, configValue: await promptText('修改配置值', String(row.configValue ?? '')) });
  if (key === 'restoreData') await restoreAction(row);
  ElMessage.success('操作已完成'); await loadPage();
}

watch(() => route.path, async () => { keyword.value = ''; pageNum.value = 1; await loadPage(); });
onMounted(loadPage);
</script>

<style scoped>
.hero-actions,.row-actions,.assist-tags{display:flex;flex-wrap:wrap;gap:10px}.hero-actions{justify-content:flex-end}.hero-actions__search{width:220px}.section-head{display:flex;justify-content:space-between;gap:16px;align-items:flex-start;margin-bottom:18px}.section-head h3{margin:0 0 6px;font-size:20px}.section-head p{margin:0;color:var(--text-secondary);line-height:1.7}.table-footer{display:flex;justify-content:flex-end;margin-top:18px}.timeline-list{display:grid;gap:14px}.timeline-item,.analysis-card{padding:16px;border-radius:var(--radius-small);background:rgba(255,255,255,.72);border:1px solid rgba(15,23,42,.06)}.timeline-item h4{margin:12px 0 6px}.timeline-item p{margin:0;color:var(--text-secondary);line-height:1.7}.analysis-grid{display:grid;gap:14px;grid-template-columns:repeat(auto-fit,minmax(140px,1fr))}.analysis-card{display:grid;gap:8px}.analysis-card strong{font-size:24px}.analysis-card span{color:var(--text-secondary)}@media (max-width:768px){.hero-actions{justify-content:flex-start}.hero-actions__search{width:100%}.section-head{flex-direction:column}}
</style>
