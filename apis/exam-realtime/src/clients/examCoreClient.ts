import type { ExamRealtimeSubmitView, HttpApiResponse, RequestContext } from '../types.js';
import { createAppError, ErrorCatalog } from '../errors.js';

interface ExamCoreSubmitRequest {
  examId: number;
  studentId: number;
  answers: Array<{
    questionId: number;
    answer: string;
  }>;
}

export class ExamCoreClient {
  constructor(private readonly baseUrl: string) {}

  async submit(payload: ExamCoreSubmitRequest, context: RequestContext): Promise<ExamRealtimeSubmitView> {
    const response = await fetch(`${this.baseUrl}/api/exam/core/submit`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        ...(context.authorization ? { Authorization: context.authorization } : {}),
        ...(context.userId !== null ? { 'X-User-Id': String(context.userId) } : {}),
        ...(context.roleId !== null ? { 'X-Role-Id': String(context.roleId) } : {}),
        'X-Request-Id': context.requestId
      },
      body: JSON.stringify(payload)
    });

    let body: HttpApiResponse<ExamRealtimeSubmitView> | undefined;
    try {
      body = (await response.json()) as HttpApiResponse<ExamRealtimeSubmitView>;
    } catch (_error) {
      body = undefined;
    }

    if (!response.ok || !body || body.code !== 200) {
      throw createAppError(
        ErrorCatalog.REALTIME_DOWNSTREAM_ERROR,
        body?.msg ?? '考试核心服务返回异常'
      );
    }

    return {
      examId: Number(body.data.examId),
      studentId: Number(body.data.studentId),
      status: String(body.data.status),
      submittedAt: String(body.data.submittedAt),
      answeredCount: Number(body.data.answeredCount)
    };
  }
}
