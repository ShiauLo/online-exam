export interface RequestContext {
  userId: number | null;
  roleId: number | null;
  requestId: string;
  authorization?: string;
}

export interface ExamSnapshot {
  examId: number;
  examName: string;
  status: string;
  paperId: number;
  duration: number;
  startTime: Date;
}

export interface ExamStudentRelation {
  relationId: number;
  classId: number;
}

export interface ScoreRecordSnapshot {
  scoreId: number;
  classId: number;
  status: string;
  submittedAt: Date | null;
}

export interface ScoreDetailSnapshot {
  detailId: number;
  questionId: number;
  sortNo: number;
  questionType: string;
  questionStem: string;
  studentAnswer: string | null;
  assignedScore: number;
}

export interface PaperQuestionSnapshot {
  questionId: number;
  sortNo: number;
  assignedScore: number;
  options: string[];
}

export interface ExamSessionSnapshot {
  exam: ExamSnapshot;
  relation: ExamStudentRelation;
  score: ScoreRecordSnapshot;
  scoreDetails: ScoreDetailSnapshot[];
  paperQuestions: PaperQuestionSnapshot[];
}

export interface DraftState {
  answers: Record<string, string>;
  currentQId?: string;
  lastSavedAt?: string;
  screenOutCount: number;
}

export interface RealtimeSessionRequest {
  examId: number;
}

export interface SaveProgressRequest {
  examId: number;
  answers: Record<string, string>;
  currentQId?: string;
}

export interface SubmitExamRequest {
  examId: number;
  answers: Record<string, string>;
}

export interface ReportAbnormalRequest {
  examId: number;
  type: string;
  desc: string;
  imgUrls?: string[];
  screenOutCount?: number;
}

export interface RealtimeQuestionView {
  questionId: number;
  stem: string;
  type: string;
  sortNo: number;
  assignedScore: number;
  options: string[];
}

export interface RealtimeSessionView {
  exam: {
    examId: number;
    examName: string;
    status: string;
    duration: number;
    remainSeconds: number;
    startTime: string;
    endTime: string;
  };
  questions: RealtimeQuestionView[];
  answers: Record<string, string>;
  currentQId?: string;
  screenOutCount: number;
}

export interface SaveProgressView {
  saved: boolean;
  savedAt: string;
  currentQId?: string;
  screenOutCount: number;
}

export interface ExamRealtimeSubmitView {
  examId: number;
  studentId: number;
  status: string;
  submittedAt: string;
  answeredCount: number;
}

export interface ExamAbnormalReportView {
  abnormalId: string;
  reportedAt: string;
}

export interface HttpApiResponse<T> {
  code: number;
  msg: string;
  data: T;
  requestId: string;
  timestamp: number;
  errors?: string[];
}

export interface AppConfig {
  app: {
    name: string;
    profiles: string;
  };
  server: {
    host: string;
    port: number;
    socketPath: string;
    issueSocketPath: string;
    corsOrigins: string;
  };
  mysql: {
    host: string;
    port: number;
    database: string;
    user: string;
    password: string;
    poolSize: number;
  };
  redis: {
    host: string;
    port: number;
    database: number;
    password: string;
  };
  nacos: {
    enabled: boolean;
    serverAddr: string;
    username: string;
    password: string;
    namespace: string;
    group: string;
    serviceIp: string;
    servicePort: number;
    heartbeatIntervalMs: number;
  };
  downstream: {
    examCoreBaseUrl: string;
  };
  jwt: {
    secret: string;
  };
  realtime: {
    ttlHoursAfterExam: number;
    autoSubmitLockSeconds: number;
    countdownIntervalMs: number;
  };
  notify: {
    defaultLimit: number;
    maxLimit: number;
    pollIntervalMs: number;
    historyLookbackMinutes: number;
  };
}

export interface SessionService {
  loadSession(request: RealtimeSessionRequest, context: RequestContext): Promise<RealtimeSessionView>;
}

export interface ProgressService {
  saveProgress(request: SaveProgressRequest, context: RequestContext): Promise<SaveProgressView>;
  syncScreenOutCount(examId: number, screenOutCount: number, context: RequestContext): Promise<number>;
}

export interface SubmitService {
  submit(request: SubmitExamRequest, context: RequestContext): Promise<ExamRealtimeSubmitView>;
}

export interface AbnormalService {
  report(request: ReportAbnormalRequest, context: RequestContext): Promise<ExamAbnormalReportView>;
}

export interface IssueNotifyPullRequest {
  cursor?: string;
  limit?: number;
}

export interface IssueNotificationItem {
  eventId: string;
  eventType: 'issueNotify' | 'processNotify';
  issueId: string;
  title: string;
  type: string;
  status: string;
  handlerId: number | null;
  handlerName?: string;
  reporterId: number;
  reporterName?: string;
  processDesc: string;
  action: 'created' | 'handled' | 'transferred' | 'closed';
  time: string;
}

export interface IssueNotifyPullView {
  scope: 'issue';
  cursor: string;
  notifications: IssueNotificationItem[];
}

export interface IssueCreatedRow {
  issueId: string;
  type: string;
  title: string;
  status: string;
  reporterId: number;
  reporterName?: string;
  handlerId: number | null;
  handlerName?: string;
  teacherIds: number[];
  description?: string;
  occurredAt: Date | string;
}

export interface IssueProcessRow {
  logId: string;
  issueId: string;
  type: string;
  title: string;
  status: string;
  reporterId: number;
  reporterName?: string;
  handlerId: number | null;
  handlerName?: string;
  teacherIds: number[];
  action: string;
  content?: string;
  occurredAt: Date | string;
}

export interface IssueNotifyRepository {
  listCreatedIssuesSince(since: Date, limit: number, preferLatest?: boolean): Promise<IssueCreatedRow[]>;
  listProcessUpdatesSince(since: Date, limit: number, preferLatest?: boolean): Promise<IssueProcessRow[]>;
}

export interface IssueNotifyService {
  resolveSubscriptionCursor(cursor?: string): string;
  pullNotifications(request: IssueNotifyPullRequest, context: RequestContext): Promise<IssueNotifyPullView>;
  pollNotifications(cursor: string, context: RequestContext, limit?: number): Promise<IssueNotifyPullView>;
}
