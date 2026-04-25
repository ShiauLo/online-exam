import { createAppError, ErrorCatalog } from '../errors.js';
import { isStudent } from '../roles.js';
import type { AppConfig, RequestContext, SaveProgressRequest, SaveProgressView } from '../types.js';
import { endTimeOf, ttlSecondsFromEnd } from './helpers.js';
import { MysqlExamReadRepository } from '../repositories/mysqlExamReadRepository.js';
import { RedisRealtimeRepository } from '../repositories/redisRealtimeRepository.js';

export class DefaultProgressService {
  constructor(
    private readonly examRepository: MysqlExamReadRepository,
    private readonly redisRepository: RedisRealtimeRepository,
    private readonly config: AppConfig
  ) {}

  async saveProgress(request: SaveProgressRequest, context: RequestContext): Promise<SaveProgressView> {
    if (!isStudent(context.roleId) || context.userId === null) {
      throw createAppError(ErrorCatalog.REALTIME_PROGRESS_FORBIDDEN);
    }

    const snapshot = await this.examRepository.getSessionSnapshot(request.examId, context.userId);
    if (!snapshot) {
      throw createAppError(ErrorCatalog.EXAM_NOT_FOUND);
    }
    if (snapshot.score.submittedAt) {
      throw createAppError(ErrorCatalog.REALTIME_SUBMIT_CONFLICT, '当前考试已正式提交');
    }

    const previous = await this.redisRepository.getDraftState(request.examId, context.userId);
    const savedAt = new Date().toISOString();
    await this.redisRepository.saveDraftState(
      request.examId,
      context.userId,
      {
        answers: request.answers,
        currentQId: request.currentQId,
        lastSavedAt: savedAt,
        screenOutCount: previous.screenOutCount
      },
      ttlSecondsFromEnd(endTimeOf(snapshot), this.config.realtime.ttlHoursAfterExam)
    );

    return {
      saved: true,
      savedAt,
      currentQId: request.currentQId,
      screenOutCount: previous.screenOutCount
    };
  }

  async syncScreenOutCount(examId: number, screenOutCount: number, context: RequestContext) {
    if (!isStudent(context.roleId) || context.userId === null) {
      throw createAppError(ErrorCatalog.REALTIME_REPORT_SCREEN_FORBIDDEN);
    }
    const snapshot = await this.examRepository.getSessionSnapshot(examId, context.userId);
    if (!snapshot) {
      throw createAppError(ErrorCatalog.EXAM_NOT_FOUND);
    }
    const current = await this.redisRepository.getDraftState(examId, context.userId);
    const nextCount = Math.max(current.screenOutCount, screenOutCount);
    await this.redisRepository.saveDraftState(
      examId,
      context.userId,
      {
        ...current,
        screenOutCount: nextCount
      },
      ttlSecondsFromEnd(endTimeOf(snapshot), this.config.realtime.ttlHoursAfterExam)
    );
    return nextCount;
  }
}
