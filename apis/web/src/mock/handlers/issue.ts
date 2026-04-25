import type { ApiResponse } from '@/types/api';
import { mockIssues, mockUsers } from '../data';
import { fail, paginate, requireCurrentUser, success, type MockRequestContext } from './shared';

export function handleIssueMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/issue/core/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockIssues];

    if (data.type) {
      list = list.filter((item) => item.type === data.type);
    }
    if (currentUser.roleType === 'student') {
      list = list.filter((item) => item.reporterId === currentUser.accountId);
    }
    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.type === 'exam');
    }
    if (currentUser.roleType === 'admin' || currentUser.roleType === 'super_admin') {
      list = list.filter((item) => item.type === 'business');
    }
    if (currentUser.roleType === 'ops') {
      list = list.filter((item) => item.type === 'system');
    }

    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/issue/core/create' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const created = {
      issueId: `i-${mockIssues.length + 1}`,
      type: String(data.type ?? 'business'),
      title: String(data.title ?? '新问题'),
      desc: String(data.desc ?? ''),
      reporterId: currentUser.accountId,
      reporterName: currentUser.realName,
      handlerId: currentUser.roleType === 'student' ? 'u-teacher-01' : currentUser.accountId,
      handlerName: currentUser.roleType === 'student' ? '周明哲' : currentUser.realName,
      status: 'pending',
      examId: String(data.examId ?? ''),
      classId: String(data.classId ?? ''),
      process: [
        {
          time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
          title: '问题已创建',
          desc: '等待处理人响应。'
        }
      ]
    } as typeof mockIssues[number];
    mockIssues.unshift(created);
    return success(created);
  }

  if (url === '/api/issue/core/handle' && method === 'PUT') {
    const issue = mockIssues.find((item) => item.issueId === String(data.issueId ?? ''));
    if (!issue) {
      fail(404, '问题不存在');
    }
    issue.status = 'processing';
    issue.process.unshift({
      time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      title: '问题处理中',
      desc: String(data.result ?? '已接单')
    });
    return success(issue);
  }

  if (url === '/api/issue/core/transfer' && method === 'PUT') {
    const issue = mockIssues.find((item) => item.issueId === String(data.issueId ?? ''));
    if (!issue) {
      fail(404, '问题不存在');
    }
    issue.handlerId = String(data.toHandlerId ?? issue.handlerId);
    issue.handlerName =
      mockUsers.find((item) => item.accountId === issue.handlerId)?.realName ?? issue.handlerName;
    issue.process.unshift({
      time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      title: '问题已转派',
      desc: String(data.reason ?? '处理人调整')
    });
    return success(issue);
  }

  if (url === '/api/issue/core/close' && method === 'PUT') {
    const issue = mockIssues.find((item) => item.issueId === String(data.issueId ?? ''));
    if (!issue) {
      fail(404, '问题不存在');
    }
    issue.status = 'closed';
    issue.process.unshift({
      time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      title: '问题已关闭',
      desc: String(data.comment ?? '申请人确认无异议')
    });
    return success(issue);
  }

  if (url === '/api/issue/core/track' && method === 'POST') {
    const issue = mockIssues.find((item) => item.issueId === String(data.issueId ?? ''));
    if (!issue) {
      fail(404, '问题不存在');
    }
    return success(issue.process);
  }

  return null;
}
