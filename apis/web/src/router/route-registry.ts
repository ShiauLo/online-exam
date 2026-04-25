import type { RouteRecordRaw, RouteRecordSingleView } from 'vue-router';
import type { RoleType } from '@/types/auth';
import type { RouteMetaConfig } from '@/types/domain';

export interface FeatureRouteConfig {
  path: string;
  name: string;
  componentKey: string;
  meta: RouteMetaConfig;
}

export const routeComponentMap: Record<string, () => Promise<unknown>> = {
  RoleHomeView: () => import('@/views/common/RoleHomeView.vue'),
  PersonalCenterView: () => import('@/views/common/PersonalCenterView.vue'),
  StudentClassView: () => import('@/views/student/StudentClassView.vue'),
  StudentExamView: () => import('@/views/student/StudentExamView.vue'),
  StudentExamAnswerView: () => import('@/views/student/StudentExamAnswerView.vue'),
  StudentScoreView: () => import('@/views/student/StudentScoreView.vue'),
  StudentIssueView: () => import('@/views/student/StudentIssueView.vue'),
  TeacherClassView: () => import('@/views/teacher/TeacherClassView.vue'),
  TeacherQuestionView: () => import('@/views/teacher/TeacherQuestionView.vue'),
  TeacherPaperView: () => import('@/views/teacher/TeacherPaperView.vue'),
  TeacherExamView: () => import('@/views/teacher/TeacherExamView.vue'),
  TeacherScoreView: () => import('@/views/teacher/TeacherScoreView.vue'),
  TeacherIssueView: () => import('@/views/teacher/TeacherIssueView.vue'),
  AdminAccountView: () => import('@/views/admin/AdminAccountView.vue'),
  AdminClassView: () => import('@/views/admin/AdminClassView.vue'),
  AdminQuestionAuditView: () => import('@/views/admin/AdminQuestionAuditView.vue'),
  AdminSystemConfigView: () => import('@/views/admin/AdminSystemConfigView.vue'),
  AdminIssueView: () => import('@/views/admin/AdminIssueView.vue'),
  AdminLogView: () => import('@/views/admin/AdminLogView.vue'),
  SuperAdminLogView: () => import('@/views/super-admin/SuperAdminLogView.vue'),
  SuperAdminRoleView: () => import('@/views/super-admin/SuperAdminRoleView.vue'),
  SuperAdminDataSecurityView: () => import('@/views/super-admin/SuperAdminDataSecurityView.vue'),
  AuditorLogView: () => import('@/views/auditor/AuditorLogView.vue'),
  AuditorScoreAuditView: () => import('@/views/auditor/AuditorScoreAuditView.vue'),
  AuditorAlarmView: () => import('@/views/auditor/AuditorAlarmView.vue'),
  AuditorIssueView: () => import('@/views/auditor/AuditorIssueView.vue'),
  OpsAlarmView: () => import('@/views/ops/OpsAlarmView.vue'),
  OpsDataSecurityView: () => import('@/views/ops/OpsDataSecurityView.vue'),
  OpsIssueView: () => import('@/views/ops/OpsIssueView.vue'),
  OpsLogView: () => import('@/views/ops/OpsLogView.vue')
};

export const featureRouteRegistry: FeatureRouteConfig[] = [
  {
    path: '/student',
    name: 'student-home',
    componentKey: 'RoleHomeView',
    meta: {
      title: '学生工作台',
      menuId: 'student-home',
      menuName: '工作台',
      icon: 'HomeFilled',
      allowedRoles: ['student'],
      requiresAuth: true
    }
  },
  {
    path: '/student/class',
    name: 'student-class',
    componentKey: 'StudentClassView',
    meta: {
      title: '我的班级',
      menuId: 'student-class',
      menuName: '我的班级',
      icon: 'CollectionTag',
      allowedRoles: ['student'],
      requiresAuth: true,
      buttonPermissions: ['class.applyJoin', 'class.quit']
    }
  },
  {
    path: '/student/exam',
    name: 'student-exam',
    componentKey: 'StudentExamView',
    meta: {
      title: '我的考试',
      menuId: 'student-exam',
      menuName: '我的考试',
      icon: 'Reading',
      allowedRoles: ['student'],
      requiresAuth: true,
      buttonPermissions: ['exam.enter']
    }
  },
  {
    path: '/student/exam/answer',
    name: 'student-exam-answer',
    componentKey: 'StudentExamAnswerView',
    meta: {
      title: '考试答题',
      allowedRoles: ['student'],
      requiresAuth: true,
      hidden: true,
      buttonPermissions: ['exam.saveProgress', 'exam.submit']
    }
  },
  {
    path: '/student/score',
    name: 'student-score',
    componentKey: 'StudentScoreView',
    meta: {
      title: '成绩查询',
      menuId: 'student-score',
      menuName: '成绩查询',
      icon: 'Histogram',
      allowedRoles: ['student'],
      requiresAuth: true,
      buttonPermissions: ['score.applyRecheck']
    }
  },
  {
    path: '/student/issue',
    name: 'student-issue',
    componentKey: 'StudentIssueView',
    meta: {
      title: '问题申报',
      menuId: 'student-issue',
      menuName: '问题申报',
      icon: 'ChatDotSquare',
      allowedRoles: ['student'],
      requiresAuth: true,
      buttonPermissions: ['issue.create']
    }
  },
  {
    path: '/teacher',
    name: 'teacher-home',
    componentKey: 'RoleHomeView',
    meta: {
      title: '教师工作台',
      menuId: 'teacher-home',
      menuName: '工作台',
      icon: 'HomeFilled',
      allowedRoles: ['teacher'],
      requiresAuth: true
    }
  },
  {
    path: '/teacher/class',
    name: 'teacher-class',
    componentKey: 'TeacherClassView',
    meta: {
      title: '我的班级',
      menuId: 'teacher-class',
      menuName: '我的班级',
      icon: 'School',
      allowedRoles: ['teacher'],
      requiresAuth: true,
      buttonPermissions: ['class.create', 'class.approveJoin', 'class.removeStudent']
    }
  },
  {
    path: '/teacher/question',
    name: 'teacher-question',
    componentKey: 'TeacherQuestionView',
    meta: {
      title: '试题管理',
      menuId: 'teacher-question',
      menuName: '试题管理',
      icon: 'Collection',
      allowedRoles: ['teacher'],
      requiresAuth: true,
      buttonPermissions: ['question.create', 'question.update', 'question.toggleStatus', 'question.import', 'question.export']
    }
  },
  {
    path: '/teacher/paper',
    name: 'teacher-paper',
    componentKey: 'TeacherPaperView',
    meta: {
      title: '试卷管理',
      menuId: 'teacher-paper',
      menuName: '试卷管理',
      icon: 'DocumentChecked',
      allowedRoles: ['teacher'],
      requiresAuth: true,
      buttonPermissions: ['paper.createManual', 'paper.createAuto', 'paper.publish', 'paper.terminate', 'paper.recycle', 'paper.export']
    }
  },
  {
    path: '/teacher/exam',
    name: 'teacher-exam',
    componentKey: 'TeacherExamView',
    meta: {
      title: '考试管理',
      menuId: 'teacher-exam',
      menuName: '考试管理',
      icon: 'Clock',
      allowedRoles: ['teacher'],
      requiresAuth: true,
      buttonPermissions: ['exam.create', 'exam.updateParams', 'exam.distribute', 'exam.toggleStatus', 'exam.approveRetest']
    }
  },
  {
    path: '/teacher/score',
    name: 'teacher-score',
    componentKey: 'TeacherScoreView',
    meta: {
      title: '成绩管理',
      menuId: 'teacher-score',
      menuName: '成绩管理',
      icon: 'TrendCharts',
      allowedRoles: ['teacher'],
      requiresAuth: true,
      buttonPermissions: ['score.manualScore', 'score.publish', 'score.export', 'score.handleAppeal']
    }
  },
  {
    path: '/teacher/issue',
    name: 'teacher-issue',
    componentKey: 'TeacherIssueView',
    meta: {
      title: '问题处理',
      menuId: 'teacher-issue',
      menuName: '问题处理',
      icon: 'Bell',
      allowedRoles: ['teacher'],
      requiresAuth: true,
      buttonPermissions: ['issue.handle', 'issue.transfer', 'issue.close']
    }
  }
];

featureRouteRegistry.push(
  {
    path: '/admin',
    name: 'admin-home',
    componentKey: 'RoleHomeView',
    meta: {
      title: '管理员工作台',
      menuId: 'admin-home',
      menuName: '工作台',
      icon: 'HomeFilled',
      allowedRoles: ['admin'],
      requiresAuth: true
    }
  },
  {
    path: '/super-admin',
    name: 'super-admin-home',
    componentKey: 'RoleHomeView',
    meta: {
      title: '超级管理员工作台',
      menuId: 'super-admin-home',
      menuName: '工作台',
      icon: 'HomeFilled',
      allowedRoles: ['super_admin'],
      requiresAuth: true
    }
  },
  {
    path: '/admin/account',
    name: 'admin-account',
    componentKey: 'AdminAccountView',
    meta: {
      title: '账户管理',
      menuId: 'admin-account',
      menuName: '账户管理',
      icon: 'User',
      allowedRoles: ['admin', 'super_admin'],
      requiresAuth: true,
      buttonPermissions: ['account.create', 'account.audit', 'account.freeze', 'account.resetPassword']
    }
  },
  {
    path: '/admin/class',
    name: 'admin-class',
    componentKey: 'AdminClassView',
    meta: {
      title: '班级管理',
      menuId: 'admin-class',
      menuName: '班级管理',
      icon: 'Notebook',
      allowedRoles: ['admin', 'super_admin'],
      requiresAuth: true,
      buttonPermissions: ['class.create', 'class.update', 'class.delete', 'class.import']
    }
  },
  {
    path: '/admin/question-audit',
    name: 'admin-question-audit',
    componentKey: 'AdminQuestionAuditView',
    meta: {
      title: '试题审核',
      menuId: 'admin-question-audit',
      menuName: '试题审核',
      icon: 'Checked',
      allowedRoles: ['admin', 'super_admin'],
      requiresAuth: true,
      buttonPermissions: ['question.audit', 'question.export']
    }
  },
  {
    path: '/admin/system-config',
    name: 'admin-system-config',
    componentKey: 'AdminSystemConfigView',
    meta: {
      title: '系统配置',
      menuId: 'admin-system-config',
      menuName: '系统配置',
      icon: 'Setting',
      allowedRoles: ['admin', 'super_admin'],
      requiresAuth: true,
      buttonPermissions: ['system.config.update', 'system.alarm.setting']
    }
  },
  {
    path: '/admin/issue',
    name: 'admin-issue',
    componentKey: 'AdminIssueView',
    meta: {
      title: '业务问题',
      menuId: 'admin-issue',
      menuName: '业务问题',
      icon: 'MessageBox',
      allowedRoles: ['admin', 'super_admin'],
      requiresAuth: true,
      buttonPermissions: ['issue.handle', 'issue.transfer', 'issue.close']
    }
  },
  {
    path: '/admin/log',
    name: 'admin-log',
    componentKey: 'AdminLogView',
    meta: {
      title: '业务日志',
      menuId: 'admin-log',
      menuName: '业务日志',
      icon: 'Document',
      allowedRoles: ['admin', 'super_admin'],
      requiresAuth: true
    }
  },
  {
    path: '/super-admin/log',
    name: 'super-admin-log',
    componentKey: 'SuperAdminLogView',
    meta: {
      title: '日志审计',
      menuId: 'super-admin-log',
      menuName: '日志审计',
      icon: 'Files',
      allowedRoles: ['super_admin'],
      requiresAuth: true,
      buttonPermissions: ['system.log.export']
    }
  },
  {
    path: '/super-admin/role',
    name: 'super-admin-role',
    componentKey: 'SuperAdminRoleView',
    meta: {
      title: '角色权限管理',
      menuId: 'super-admin-role',
      menuName: '角色权限管理',
      icon: 'Lock',
      allowedRoles: ['super_admin'],
      requiresAuth: true,
      buttonPermissions: ['system.role.save', 'system.permission.assign']
    }
  },
  {
    path: '/super-admin/data-security',
    name: 'super-admin-data-security',
    componentKey: 'SuperAdminDataSecurityView',
    meta: {
      title: '数据安全中心',
      menuId: 'super-admin-data-security',
      menuName: '数据安全中心',
      icon: 'FolderChecked',
      allowedRoles: ['super_admin'],
      requiresAuth: true,
      buttonPermissions: ['system.data.backup', 'system.data.restore']
    }
  }
);

featureRouteRegistry.push(
  {
    path: '/auditor',
    name: 'auditor-home',
    componentKey: 'RoleHomeView',
    meta: {
      title: '审计员工作台',
      menuId: 'auditor-home',
      menuName: '工作台',
      icon: 'HomeFilled',
      allowedRoles: ['auditor'],
      requiresAuth: true
    }
  },
  {
    path: '/auditor/log',
    name: 'auditor-log',
    componentKey: 'AuditorLogView',
    meta: {
      title: '日志审计',
      menuId: 'auditor-log',
      menuName: '日志审计',
      icon: 'Tickets',
      allowedRoles: ['auditor'],
      requiresAuth: true,
      buttonPermissions: ['system.log.export']
    }
  },
  {
    path: '/auditor/score-audit',
    name: 'auditor-score-audit',
    componentKey: 'AuditorScoreAuditView',
    meta: {
      title: '成绩核查',
      menuId: 'auditor-score-audit',
      menuName: '成绩核查',
      icon: 'DataAnalysis',
      allowedRoles: ['auditor'],
      requiresAuth: true
    }
  },
  {
    path: '/auditor/alarm',
    name: 'auditor-alarm',
    componentKey: 'AuditorAlarmView',
    meta: {
      title: '异常行为监控',
      menuId: 'auditor-alarm',
      menuName: '异常行为监控',
      icon: 'WarningFilled',
      allowedRoles: ['auditor'],
      requiresAuth: true
    }
  },
  {
    path: '/auditor/issue',
    name: 'auditor-issue',
    componentKey: 'AuditorIssueView',
    meta: {
      title: '问题跟踪',
      menuId: 'auditor-issue',
      menuName: '问题跟踪',
      icon: 'Connection',
      allowedRoles: ['auditor'],
      requiresAuth: true
    }
  },
  {
    path: '/ops',
    name: 'ops-home',
    componentKey: 'RoleHomeView',
    meta: {
      title: '系统运维工作台',
      menuId: 'ops-home',
      menuName: '工作台',
      icon: 'HomeFilled',
      allowedRoles: ['ops'],
      requiresAuth: true
    }
  },
  {
    path: '/ops/alarm',
    name: 'ops-alarm',
    componentKey: 'OpsAlarmView',
    meta: {
      title: '系统告警',
      menuId: 'ops-alarm',
      menuName: '系统告警',
      icon: 'BellFilled',
      allowedRoles: ['ops'],
      requiresAuth: true
    }
  },
  {
    path: '/ops/data-security',
    name: 'ops-data-security',
    componentKey: 'OpsDataSecurityView',
    meta: {
      title: '数据安全中心',
      menuId: 'ops-data-security',
      menuName: '数据安全中心',
      icon: 'FolderOpened',
      allowedRoles: ['ops'],
      requiresAuth: true,
      buttonPermissions: ['system.data.backup']
    }
  },
  {
    path: '/ops/issue',
    name: 'ops-issue',
    componentKey: 'OpsIssueView',
    meta: {
      title: '系统类问题',
      menuId: 'ops-issue',
      menuName: '系统类问题',
      icon: 'Tools',
      allowedRoles: ['ops'],
      requiresAuth: true,
      buttonPermissions: ['issue.handle', 'issue.transfer', 'issue.close']
    }
  },
  {
    path: '/ops/log',
    name: 'ops-log',
    componentKey: 'OpsLogView',
    meta: {
      title: '系统日志',
      menuId: 'ops-log',
      menuName: '系统日志',
      icon: 'Memo',
      allowedRoles: ['ops'],
      requiresAuth: true
    }
  },
  {
    path: '/personal',
    name: 'personal-center',
    componentKey: 'PersonalCenterView',
    meta: {
      title: '个人中心',
      menuId: 'personal',
      menuName: '个人中心',
      icon: 'Avatar',
      allowedRoles: ['student', 'teacher', 'admin', 'super_admin', 'auditor', 'ops'],
      requiresAuth: true
    }
  }
);

export const roleRoutePaths: Record<RoleType, string[]> = {
  student: ['/student', '/student/class', '/student/exam', '/student/exam/answer', '/student/score', '/student/issue', '/personal'],
  teacher: ['/teacher', '/teacher/class', '/teacher/question', '/teacher/paper', '/teacher/exam', '/teacher/score', '/teacher/issue', '/personal'],
  admin: ['/admin', '/admin/account', '/admin/class', '/admin/question-audit', '/admin/system-config', '/admin/issue', '/admin/log', '/personal'],
  super_admin: ['/super-admin', '/admin/account', '/admin/class', '/admin/question-audit', '/admin/system-config', '/admin/issue', '/admin/log', '/super-admin/log', '/super-admin/role', '/super-admin/data-security', '/personal'],
  auditor: ['/auditor', '/auditor/log', '/auditor/score-audit', '/auditor/alarm', '/auditor/issue', '/personal'],
  ops: ['/ops', '/ops/alarm', '/ops/data-security', '/ops/issue', '/ops/log', '/personal']
};

export const roleButtonPermissions: Record<RoleType, string[]> = {
  student: ['class.applyJoin', 'class.quit', 'exam.enter', 'exam.saveProgress', 'exam.submit', 'score.applyRecheck', 'issue.create'],
  teacher: ['class.create', 'class.approveJoin', 'class.removeStudent', 'question.create', 'question.update', 'question.toggleStatus', 'question.import', 'question.export', 'paper.createManual', 'paper.createAuto', 'paper.publish', 'paper.terminate', 'paper.recycle', 'paper.export', 'exam.create', 'exam.updateParams', 'exam.distribute', 'exam.toggleStatus', 'exam.approveRetest', 'score.manualScore', 'score.publish', 'score.export', 'score.handleAppeal', 'issue.handle', 'issue.transfer', 'issue.close'],
  admin: ['account.create', 'account.audit', 'account.freeze', 'account.resetPassword', 'class.create', 'class.update', 'class.delete', 'class.import', 'question.audit', 'question.export', 'issue.handle', 'issue.transfer', 'issue.close'],
  super_admin: ['account.create', 'account.audit', 'account.freeze', 'account.resetPassword', 'class.create', 'class.update', 'class.delete', 'class.import', 'question.audit', 'question.export', 'issue.handle', 'issue.transfer', 'issue.close', 'system.config.update', 'system.alarm.setting', 'system.log.export', 'system.role.save', 'system.permission.assign', 'system.data.backup', 'system.data.restore'],
  auditor: ['system.log.export'],
  ops: ['system.data.backup', 'issue.handle', 'issue.transfer', 'issue.close']
};

export function getRoutesByRole(role: RoleType) {
  const allowed = new Set(roleRoutePaths[role]);
  return featureRouteRegistry.filter((item) => allowed.has(item.path));
}

export function getLandingPath(role: RoleType) {
  return roleRoutePaths[role][0];
}

export function toRouteRecord(config: FeatureRouteConfig): RouteRecordSingleView {
  const component = routeComponentMap[config.componentKey];
  if (!component) {
    throw new Error(`未找到路由组件映射: ${config.componentKey}`);
  }

  const resolvedComponent = component as Exclude<
    RouteRecordSingleView['component'],
    null | undefined
  >;

  return {
    path: config.path.replace(/^\//, ''),
    name: config.name,
    component: resolvedComponent,
    meta: config.meta as unknown as Record<PropertyKey, unknown>
  };
}
