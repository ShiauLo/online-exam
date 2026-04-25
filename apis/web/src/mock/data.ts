import type { RoleType, UserProfile } from '@/types/auth';
import type { ExamStatus, IssueType, QuestionType } from '@/types/domain';

export interface MockUser extends UserProfile {
  password: string;
  status: 'active' | 'pending' | 'frozen';
}

export interface MockClass {
  classId: string;
  className: string;
  description: string;
  teacherId: string;
  teacherName: string;
  classCode: string;
  approvedMemberCount: number;
  pendingMemberCount: number;
  isMandatory: boolean;
  createdBy: string;
  createTime: string;
  updateTime: string;
  students: string[];
  pendingStudents: string[];
}

export interface MockQuestion {
  questionId: string;
  content: string;
  type: QuestionType;
  categoryId: string;
  categoryName: string;
  creatorId: string;
  creatorName: string;
  auditStatus: 'pending' | 'approved' | 'rejected';
  isDisabled: boolean;
  referenceLocked: boolean;
  difficulty: 'easy' | 'medium' | 'hard';
  options?: string[];
  answer: string;
  analysis: string;
}

export interface MockCategory {
  categoryId: string;
  name: string;
  creatorId: string;
  isPersonal: boolean;
  isDisabled: boolean;
}

export interface MockPaper {
  paperId: string;
  paperName: string;
  creatorId: string;
  creatorName: string;
  examTime: number;
  passScore: number;
  questionIds: string[];
  status: ExamStatus;
  publishScope: string[];
}

export interface MockExam {
  examId: string;
  examName: string;
  paperId: string;
  classIds: string[];
  classNames: string[];
  startTime: string;
  duration: number;
  status: ExamStatus;
  creatorId: string;
  creatorName: string;
  answeredCount: number;
  totalCount: number;
  isPaused: boolean;
}

export interface MockExamQuestion {
  questionId: string;
  stem: string;
  type: QuestionType;
  options?: string[];
  answer: string;
}

export interface MockScore {
  scoreId: string;
  examId: string;
  examName: string;
  classId: string;
  className: string;
  studentId: string;
  studentName: string;
  totalScore: number;
  objectiveScore: number;
  subjectiveScore: number;
  publishStatus: 'draft' | 'published';
  recheckStatus?: 'none' | 'pending' | 'processed';
  detail: Array<{ questionId: string; stem: string; score: number; comment?: string }>;
}

export interface MockIssue {
  issueId: string;
  type: IssueType;
  title: string;
  desc: string;
  reporterId: string;
  reporterName: string;
  handlerId: string;
  handlerName: string;
  status: 'pending' | 'processing' | 'closed';
  examId?: string;
  classId?: string;
  process: Array<{ time: string; title: string; desc: string }>;
}

export interface MockSystemConfig {
  configKey: string;
  category: string;
  configValue: string;
  desc: string;
}

export interface MockAlarm {
  alarmId: string;
  alarmType: string;
  level: 'low' | 'medium' | 'high';
  status: 'new' | 'processing' | 'resolved';
  threshold: string;
  recipients: string[];
  createTime: string;
}

export interface MockLog {
  logId: string;
  logType: 'business' | 'system' | 'audit';
  operator: string;
  roleType: RoleType;
  requestId: string;
  action: string;
  result: string;
  time: string;
}

export interface MockBackup {
  backupId: string;
  backupType: 'full' | 'incremental' | 'config' | 'audit';
  status: 'SUCCESS' | 'RUNNING' | 'FAILED';
  lifecycleStage: string;
  updateTime: string;
  canRestore: boolean;
}

export interface MockRoleTemplate {
  roleId: string;
  roleName: string;
  permissionCount: number;
  expireTime?: string;
}

function nowString(offsetHour = 0) {
  const date = new Date(Date.now() + offsetHour * 60 * 60 * 1000);
  const pad = (value: number) => `${value}`.padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export const mockUsers: MockUser[] = [
  { accountId: 'u-student-01', username: 'student01', realName: '林若溪', roleType: 'student', phone: '13810002001', email: 'student01@example.com', className: 'Java 开发一班', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' },
  { accountId: 'u-teacher-01', username: 'teacher01', realName: '周明哲', roleType: 'teacher', phone: '13910003001', email: 'teacher01@example.com', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' },
  { accountId: 'u-admin-01', username: 'admin01', realName: '顾清和', roleType: 'admin', phone: '13710004001', email: 'admin01@example.com', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' },
  { accountId: 'u-super-01', username: 'super01', realName: '沈嘉屿', roleType: 'super_admin', phone: '13610005001', email: 'super01@example.com', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' },
  { accountId: 'u-auditor-01', username: 'auditor01', realName: '何听澜', roleType: 'auditor', phone: '13510006001', email: 'auditor01@example.com', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' },
  { accountId: 'u-ops-01', username: 'ops01', realName: '陆川', roleType: 'ops', phone: '13410007001', email: 'ops01@example.com', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' },
  { accountId: 'u-student-02', username: 'student02', realName: '程亦安', roleType: 'student', phone: '13810002002', email: 'student02@example.com', className: 'Java 开发一班', password: 'Exam@123', demoPassword: 'Exam@123', status: 'pending' },
  { accountId: 'u-teacher-02', username: 'teacher02', realName: '黄一白', roleType: 'teacher', phone: '13910003002', email: 'teacher02@example.com', password: 'Exam@123', demoPassword: 'Exam@123', status: 'active' }
];

export const mockClasses: MockClass[] = [
  { classId: 'c-101', className: 'Java 开发一班', description: '基础能力与阶段测试班级', teacherId: 'u-teacher-01', teacherName: '周明哲', classCode: 'JAVA101', approvedMemberCount: 32, pendingMemberCount: 2, isMandatory: false, createdBy: 'teacher01', createTime: nowString(-120), updateTime: nowString(-2), students: ['u-student-01'], pendingStudents: ['u-student-02'] },
  { classId: 'c-202', className: '数据结构冲刺班', description: '期末复习与补考辅导', teacherId: 'u-teacher-02', teacherName: '黄一白', classCode: 'DS202', approvedMemberCount: 27, pendingMemberCount: 0, isMandatory: true, createdBy: 'admin01', createTime: nowString(-240), updateTime: nowString(-12), students: [], pendingStudents: [] }
];

export const mockCategories: MockCategory[] = [
  { categoryId: 'qc-1', name: 'Java 基础', creatorId: 'u-teacher-01', isPersonal: true, isDisabled: false },
  { categoryId: 'qc-2', name: '数据结构', creatorId: 'u-admin-01', isPersonal: false, isDisabled: false },
  { categoryId: 'qc-3', name: '算法综合', creatorId: 'u-teacher-01', isPersonal: true, isDisabled: false }
];

export const mockQuestions: MockQuestion[] = [
  { questionId: 'q-1', content: 'Java 中接口与抽象类的主要区别是什么？', type: 'single', categoryId: 'qc-1', categoryName: 'Java 基础', creatorId: 'u-teacher-01', creatorName: '周明哲', auditStatus: 'approved', isDisabled: false, referenceLocked: true, difficulty: 'medium', options: ['只能单继承', '接口不允许定义常量', '抽象类不能有实现方法', '接口不能被实现'], answer: '只能单继承', analysis: '抽象类可含实现方法，接口支持常量和抽象能力。' },
  { questionId: 'q-2', content: '二叉树前序遍历的访问顺序是？', type: 'single', categoryId: 'qc-2', categoryName: '数据结构', creatorId: 'u-admin-01', creatorName: '顾清和', auditStatus: 'pending', isDisabled: false, referenceLocked: false, difficulty: 'easy', options: ['根左右', '左根右', '左右根', '右左根'], answer: '根左右', analysis: '前序遍历为根节点先访问。' },
  { questionId: 'q-3', content: '请说明 HashMap 扩容时需要关注的两个风险点。', type: 'subjective', categoryId: 'qc-1', categoryName: 'Java 基础', creatorId: 'u-teacher-01', creatorName: '周明哲', auditStatus: 'approved', isDisabled: false, referenceLocked: false, difficulty: 'hard', answer: '数据迁移和线程安全问题', analysis: '关注 rehash 成本与并发可见性。' }
];

export const mockPapers: MockPaper[] = [
  { paperId: 'p-1', paperName: 'Java 基础摸底卷', creatorId: 'u-teacher-01', creatorName: '周明哲', examTime: 90, passScore: 60, questionIds: ['q-1', 'q-3'], status: 'published', publishScope: ['c-101'] },
  { paperId: 'p-2', paperName: '数据结构期末卷', creatorId: 'u-admin-01', creatorName: '顾清和', examTime: 120, passScore: 72, questionIds: ['q-2'], status: 'draft', publishScope: ['c-202'] }
];

export const mockExams: MockExam[] = [
  { examId: 'e-1', examName: 'Java 阶段测验', paperId: 'p-1', classIds: ['c-101'], classNames: ['Java 开发一班'], startTime: nowString(-1), duration: 90, status: 'underway', creatorId: 'u-teacher-01', creatorName: '周明哲', answeredCount: 18, totalCount: 32, isPaused: false },
  { examId: 'e-2', examName: '数据结构补考', paperId: 'p-2', classIds: ['c-202'], classNames: ['数据结构冲刺班'], startTime: nowString(24), duration: 120, status: 'published', creatorId: 'u-admin-01', creatorName: '顾清和', answeredCount: 0, totalCount: 27, isPaused: false }
];

export const mockExamQuestionMap: Record<string, MockExamQuestion[]> = {
  'e-1': [
    { questionId: 'q-1', stem: 'Java 中接口与抽象类的主要区别是什么？', type: 'single', options: ['只能单继承', '接口不允许定义常量', '抽象类不能有实现方法', '接口不能被实现'], answer: '只能单继承' },
    { questionId: 'q-3', stem: '请说明 HashMap 扩容时需要关注的两个风险点。', type: 'subjective', answer: '数据迁移和线程安全问题' }
  ]
};

export const mockScores: MockScore[] = [
  { scoreId: 's-1', examId: 'e-1', examName: 'Java 阶段测验', classId: 'c-101', className: 'Java 开发一班', studentId: 'u-student-01', studentName: '林若溪', totalScore: 86, objectiveScore: 46, subjectiveScore: 40, publishStatus: 'published', recheckStatus: 'none', detail: [{ questionId: 'q-1', stem: 'Java 中接口与抽象类的主要区别是什么？', score: 46 }, { questionId: 'q-3', stem: '请说明 HashMap 扩容时需要关注的两个风险点。', score: 40, comment: '论述完整，逻辑清楚。' }] }
];

export const mockIssues: MockIssue[] = [
  { issueId: 'i-1', type: 'exam', title: '考试中切屏后页面卡顿', desc: '切换窗口后倒计时冻结 5 秒。', reporterId: 'u-student-01', reporterName: '林若溪', handlerId: 'u-teacher-01', handlerName: '周明哲', status: 'processing', examId: 'e-1', classId: 'c-101', process: [{ time: nowString(-4), title: '学生提交问题', desc: '上传了异常截图。' }, { time: nowString(-3), title: '教师接单', desc: '正在核对实时日志。' }] },
  { issueId: 'i-2', type: 'business', title: '教师注册审核待处理', desc: '新教师账号需要审核启用。', reporterId: 'u-admin-01', reporterName: '顾清和', handlerId: 'u-super-01', handlerName: '沈嘉屿', status: 'pending', process: [{ time: nowString(-10), title: '管理员发起', desc: '等待超级管理员确认。' }] },
  { issueId: 'i-3', type: 'system', title: '备份任务执行时间过长', desc: '凌晨增量备份超过阈值 20 分钟。', reporterId: 'u-ops-01', reporterName: '陆川', handlerId: 'u-ops-01', handlerName: '陆川', status: 'processing', process: [{ time: nowString(-6), title: '系统自动告警', desc: '备份链路触发高耗时预警。' }, { time: nowString(-5), title: '运维处理中', desc: '正在排查磁盘 IO。' }] }
];

export const mockSystemConfigs: MockSystemConfig[] = [
  { configKey: 'exam.autoSubmit.seconds', category: 'exam', configValue: '30', desc: '考试超时自动提交缓冲秒数' },
  { configKey: 'security.login.maxFailure', category: 'security', configValue: '5', desc: '登录失败触发验证码阈值' },
  { configKey: 'issue.timeout.notify', category: 'issue', configValue: '15', desc: '问题处理超时提醒分钟数' }
];

export const mockAlarms: MockAlarm[] = [
  { alarmId: 'a-1', alarmType: 'screen-out', level: 'high', status: 'new', threshold: '5 次切屏', recipients: ['周明哲', '何听澜'], createTime: nowString(-2) },
  { alarmId: 'a-2', alarmType: 'backup-duration', level: 'medium', status: 'processing', threshold: '20 分钟', recipients: ['陆川'], createTime: nowString(-5) }
];

export const mockLogs: MockLog[] = [
  { logId: 'l-1', logType: 'business', operator: 'teacher01', roleType: 'teacher', requestId: 'req-0001', action: '审批学生入班', result: 'success', time: nowString(-8) },
  { logId: 'l-2', logType: 'system', operator: 'ops01', roleType: 'ops', requestId: 'req-0002', action: '执行增量备份', result: 'success', time: nowString(-6) },
  { logId: 'l-3', logType: 'audit', operator: 'auditor01', roleType: 'auditor', requestId: 'req-0003', action: '导出日志审批单', result: 'pending', time: nowString(-3) }
];

export const mockBackups: MockBackup[] = [
  { backupId: 'b-1', backupType: 'full', status: 'SUCCESS', lifecycleStage: '已归档', updateTime: nowString(-24), canRestore: true },
  { backupId: 'b-2', backupType: 'incremental', status: 'RUNNING', lifecycleStage: '执行中', updateTime: nowString(-1), canRestore: false }
];

export const mockRoleTemplates: MockRoleTemplate[] = [
  { roleId: 'role-1', roleName: '教师标准权限', permissionCount: 18 },
  { roleId: 'role-2', roleName: '审计临时核查权限', permissionCount: 9, expireTime: nowString(24 * 7) }
];

export const loginFailureCounter: Record<string, number> = {};
export const accessTokenMap: Record<string, string> = {};
export const refreshTokenMap: Record<string, string> = {};
export const mockAnswerProgress: Record<string, Record<string, string>> = {};
export const smsCodeMap: Record<string, string> = {};

export function buildRequestId() {
  return `req-${Math.random().toString(36).slice(2, 10)}`;
}

export function buildToken(prefix: string, accountId: string) {
  return `${prefix}-${accountId}-${Math.random().toString(36).slice(2, 10)}`;
}

export function resolveAccountIdFromToken(token: string, prefix: 'access' | 'refresh') {
  const tokenPrefix = `${prefix}-`;
  if (!token.startsWith(tokenPrefix)) {
    return undefined;
  }

  const body = token.slice(tokenPrefix.length);
  const lastSeparatorIndex = body.lastIndexOf('-');
  if (lastSeparatorIndex <= 0) {
    return undefined;
  }

  return body.slice(0, lastSeparatorIndex);
}

export function findUserByAccount(account: string) {
  return mockUsers.find((item) => item.username === account || item.email === account || item.phone === account);
}

export function getUserByToken(token?: string) {
  if (!token) {
    return undefined;
  }
  const accountId = accessTokenMap[token] ?? resolveAccountIdFromToken(token, 'access');
  if (accountId) {
    accessTokenMap[token] = accountId;
  }
  return mockUsers.find((item) => item.accountId === accountId);
}
