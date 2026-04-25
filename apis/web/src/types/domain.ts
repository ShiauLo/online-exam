import type { RoleType } from './auth';

export type QuestionType = 'single' | 'multi' | 'subjective';
export type ExamStatus =
  | 'draft'
  | 'published'
  | 'underway'
  | 'paused'
  | 'ended'
  | 'terminated'
  | 'recycled';
export type IssueType = 'business' | 'exam' | 'system';

export interface MetricItem {
  label: string;
  value: string | number;
  trend?: string;
  state?: 'default' | 'warning' | 'danger' | 'success';
}

export interface TimelineItem {
  time: string;
  title: string;
  desc: string;
}

export interface AlertItem {
  title: string;
  level: 'info' | 'warning' | 'error' | 'success';
  desc: string;
}

export interface SelectOption {
  label: string;
  value: string;
}

export interface RouteMetaConfig extends Record<string, unknown> {
  title: string;
  menuId?: string;
  menuName?: string;
  icon?: string;
  requiresAuth?: boolean;
  allowedRoles?: RoleType[];
  buttonPermissions?: string[];
  hidden?: boolean;
}
