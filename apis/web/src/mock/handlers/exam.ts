import type { ApiResponse } from '@/types/api';
import { mockAnswerProgress, mockClasses, mockExamQuestionMap, mockExams, mockIssues } from '../data';
import { fail, paginate, requireCurrentUser, success, type MockRequestContext } from './shared';

export function handleExamMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/exam/core/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockExams];

    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.creatorId === currentUser.accountId);
    }

    if (currentUser.roleType === 'student') {
      list = list.filter((item) =>
        item.classIds.some((classId) =>
          mockClasses.find((cls) => cls.classId === classId)?.students.includes(currentUser.accountId)
        )
      );
    }

    if (data.status) {
      list = list.filter((item) => item.status === data.status);
    }

    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/exam/core/create' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const classIds = (data.classIds as string[]) ?? [];
    const record = {
      examId: `e-${mockExams.length + 1}`,
      examName: String(data.examName ?? '新考试'),
      paperId: String(data.paperId ?? ''),
      classIds,
      classNames: classIds.map((item) => mockClasses.find((cls) => cls.classId === item)?.className ?? item),
      startTime: String(data.startTime ?? new Date().toISOString()),
      duration: Number(data.duration ?? 90),
      status: 'published',
      creatorId: currentUser.accountId,
      creatorName: currentUser.realName,
      answeredCount: 0,
      totalCount: classIds.length * 10,
      isPaused: false
    } as typeof mockExams[number];
    mockExams.unshift(record);
    return success(record);
  }

  if (url === '/api/exam/core/update/params' && method === 'PUT') {
    const record = mockExams.find((item) => item.examId === String(data.examId ?? ''));
    if (!record) {
      fail(404, '考试不存在');
    }
    record.duration = Number(data.duration ?? record.duration);
    return success(record);
  }

  if (url === '/api/exam/core/distribute' && method === 'PUT') {
    return success(true);
  }

  if (url === '/api/exam/core/toggle-status' && method === 'PUT') {
    const record = mockExams.find((item) => item.examId === String(data.examId ?? ''));
    if (!record) {
      fail(404, '考试不存在');
    }
    const isPaused = Boolean(data.isPaused);
    record.isPaused = isPaused;
    record.status = isPaused ? 'paused' : 'underway';
    return success(record);
  }

  if (url === '/api/exam/core/approve-retest' && method === 'PUT') {
    return success(true);
  }

  if (url === '/api/exam/realtime/session' && method === 'POST') {
    const examId = String(data.examId ?? '');
    return success({
      exam: mockExams.find((item) => item.examId === examId),
      questions: mockExamQuestionMap[examId] ?? []
    });
  }

  if (url === '/api/exam/realtime/save-progress' && method === 'POST') {
    mockAnswerProgress[String(data.examId ?? '')] = (data.answers as Record<string, string>) ?? {};
    return success(true);
  }

  if (url === '/api/exam/realtime/submit' && method === 'PUT') {
    const examId = String(data.examId ?? '');
    mockAnswerProgress[examId] = (data.answers as Record<string, string>) ?? {};
    const exam = mockExams.find((item) => item.examId === examId);
    if (exam) {
      exam.answeredCount += 1;
    }
    return success({ submitted: true });
  }

  if (url === '/api/exam/realtime/report-abnormal' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    mockIssues.unshift({
      issueId: `i-${mockIssues.length + 1}`,
      type: 'exam',
      title: String(data.type ?? '考试异常'),
      desc: String(data.desc ?? ''),
      reporterId: currentUser.accountId,
      reporterName: currentUser.realName,
      handlerId: 'u-teacher-01',
      handlerName: '周明哲',
      status: 'pending',
      examId: String(data.examId ?? ''),
      process: [
        {
          time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
          title: '异常已上报',
          desc: '等待教师处理。'
        }
      ]
    });
    return success(true);
  }

  return null;
}
