import type { ApiResponse } from '@/types/api';
import { mockPapers, mockQuestions } from '../data';
import { fail, paginate, requireCurrentUser, success, type MockRequestContext } from './shared';

export function handlePaperMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/paper/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockPapers];
    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.creatorId === currentUser.accountId);
    }
    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/paper/create/manual' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const record = {
      paperId: `p-${mockPapers.length + 1}`,
      paperName: String(data.paperName ?? '手动组卷'),
      creatorId: currentUser.accountId,
      creatorName: currentUser.realName,
      examTime: Number(data.examTime ?? 90),
      passScore: Number(data.passScore ?? 60),
      questionIds: (data.questionIds as string[]) ?? [],
      status: 'draft',
      publishScope: []
    } as typeof mockPapers[number];
    mockPapers.unshift(record);
    return success(record);
  }

  if (url === '/api/paper/create/auto' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const record = {
      paperId: `p-${mockPapers.length + 1}`,
      paperName: String(data.paperName ?? '自动组卷'),
      creatorId: currentUser.accountId,
      creatorName: currentUser.realName,
      examTime: 90,
      passScore: 60,
      questionIds: mockQuestions.slice(0, 2).map((item) => item.questionId),
      status: 'draft',
      publishScope: []
    } as typeof mockPapers[number];
    mockPapers.unshift(record);
    return success(record);
  }

  if (url === '/api/paper/publish' && method === 'PUT') {
    const record = mockPapers.find((item) => item.paperId === String(data.paperId ?? ''));
    if (!record) {
      fail(404, '试卷不存在');
    }
    record.status = 'published';
    record.publishScope = (data.classIds as string[]) ?? [];
    return success(true);
  }

  if (url === '/api/paper/terminate' && method === 'PUT') {
    const record = mockPapers.find((item) => item.paperId === String(data.paperId ?? ''));
    if (!record) {
      fail(404, '试卷不存在');
    }
    record.status = 'terminated';
    return success(true);
  }

  if (url === '/api/paper/recycle' && method === 'PUT') {
    const record = mockPapers.find((item) => item.paperId === String(data.paperId ?? ''));
    if (!record) {
      fail(404, '试卷不存在');
    }
    record.status = 'recycled';
    return success(true);
  }

  if (url === '/api/paper/export' && method === 'GET') {
    return success({ fileKey: 'paper-export-001' });
  }

  return null;
}
