import type { ApiResponse } from '@/types/api';
import { mockCategories, mockQuestions } from '../data';
import {
  applyQueryFilter,
  containsRole,
  fail,
  paginate,
  requireCurrentUser,
  success,
  type MockRequestContext
} from './shared';

export function handleQuestionMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/question/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockQuestions];

    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.creatorId === currentUser.accountId);
    }

    if (data.auditStatus) {
      list = list.filter((item) => item.auditStatus === data.auditStatus);
    }

    list = applyQueryFilter(list, String(data.keyword ?? ''), (item) => [
      item.content,
      item.categoryName,
      item.creatorName
    ]);

    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/question/category/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockCategories];
    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.creatorId === currentUser.accountId || !item.isPersonal);
    }
    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/question/create' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const record = {
      questionId: `q-${mockQuestions.length + 1}`,
      content: String(data.content ?? '新试题'),
      type: String(data.type ?? 'single'),
      categoryId: String(data.categoryId ?? 'qc-1'),
      categoryName:
        mockCategories.find((item) => item.categoryId === String(data.categoryId ?? 'qc-1'))
          ?.name ?? '未分类',
      creatorId: currentUser.accountId,
      creatorName: currentUser.realName,
      auditStatus: containsRole(['admin', 'super_admin'], currentUser.roleType)
        ? 'approved'
        : 'pending',
      isDisabled: false,
      referenceLocked: false,
      difficulty: String(data.difficulty ?? 'medium'),
      options: (data.options as string[]) ?? [],
      answer: String(data.answer ?? ''),
      analysis: String(data.analysis ?? '')
    } as typeof mockQuestions[number];

    mockQuestions.unshift(record);
    return success(record);
  }

  if (url === '/api/question/update' && method === 'PUT') {
    const record = mockQuestions.find((item) => item.questionId === String(data.questionId ?? ''));
    if (!record) {
      fail(404, '试题不存在');
    }
    if (record.referenceLocked) {
      fail(409, '被引用试题不可修改关键字段');
    }
    record.content = String(data.content ?? record.content);
    record.analysis = String(data.analysis ?? record.analysis);
    record.answer = String(data.answer ?? record.answer);
    return success(record);
  }

  if (url === '/api/question/toggle-status' && method === 'PUT') {
    const record = mockQuestions.find((item) => item.questionId === String(data.questionId ?? ''));
    if (!record) {
      fail(404, '试题不存在');
    }
    record.isDisabled = Boolean(data.isDisabled);
    return success(true);
  }

  if (url === '/api/question/import' && method === 'POST') {
    return success({
      totalCount: 12,
      successCount: 11,
      failedCount: 1,
      rowErrors: ['第 4 行答案格式错误']
    });
  }

  if (url === '/api/question/export' && method === 'GET') {
    return success({ fileKey: 'question-export-001' });
  }

  if (url === '/api/question/audit' && method === 'PUT') {
    const record = mockQuestions.find((item) => item.questionId === String(data.questionId ?? ''));
    if (!record) {
      fail(404, '试题不存在');
    }
    record.auditStatus = String(data.auditResult ?? 'reject') === 'approve' ? 'approved' : 'rejected';
    return success(true);
  }

  return null;
}
