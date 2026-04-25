import { createAppError, ErrorCatalog } from '../errors.js';
import { isStudent } from '../roles.js';
import type { AppConfig, RequestContext, SubmitExamRequest } from '../types.js';
import { endTimeOf, ttlSecondsFromEnd } from './helpers.js';
import { MysqlExamReadRepository } from '../repositories/mysqlExamReadRepository.js';
import { RedisRealtimeRepository } from '../repositories/redisRealtimeRepository.js';
import { ExamCoreClient } from '../clients/examCoreClient.js';

export class DefaultSubmitService {
  constructor(
    private readonly examRepository: MysqlExamReadRepository,
    private readonly redisRepository: RedisRealtimeRepository,
    private readonly examCoreClient: ExamCoreClient,
    private readonly config: AppConfig
  ) {}

  async submit(request: SubmitExamRequest, context: RequestContext) {
    if (!isStudent(context.roleId) || context.userId === null) {
      throw createAppError(ErrorCatalog.REALTIME_SESSION_FORBIDDEN);
    }

    const snapshot = await this.examRepository.getSessionSnapshot(request.examId, context.userId);
    if (!snapshot) {
      throw createAppError(ErrorCatalog.EXAM_NOT_FOUND);
    }
    if (snapshot.score.submittedAt) {
      throw createAppError(ErrorCatalog.REALTIME_SUBMIT_CONFLICT, '当前考试已正式提交');
    }

    const lockAcquired = await this.redisRepository.acquireSubmitLock(
      request.examId,
      context.userId,
      this.config.realtime.autoSubmitLockSeconds
    );
    if (!lockAcquired) {
      throw createAppError(ErrorCatalog.REALTIME_SUBMIT_CONFLICT, '当前考试正在提交中');
    }

    try {
      const draftState = await this.redisRepository.getDraftState(request.examId, context.userId);
      const mergedAnswers = {
        ...draftState.answers,
        ...request.answers
      };
      const answerItems = snapshot.scoreDetails.map((detail) => ({
        questionId: detail.questionId,
        answer: String(mergedAnswers[String(detail.questionId)] ?? '')
      }));

      const result = await this.examCoreClient.submit(
        {
          examId: request.examId,
          studentId: context.userId,
          answers: answerItems
        },
        context
      );

      await this.redisRepository.clearDraftState(request.examId, context.userId);
      await this.redisRepository.clearActiveSocketId(request.examId, context.userId);
      return result;
    } catch (error) {
      await this.redisRepository.releaseSubmitLock(request.examId, context.userId);
      throw error;
    }
  }

  async autoSubmit(examId: number, context: RequestContext) {
    const snapshot = await this.examRepository.getSessionSnapshot(examId, Number(context.userId));
    if (!snapshot || snapshot.score.submittedAt) {
      return null;
    }
    const draftState = await this.redisRepository.getDraftState(examId, Number(context.userId));
    const answers = draftState.answers ?? {};
    return this.submit(
      {
        examId,
        answers
      },
      context
    );
  }

  ttlSeconds(examId: number, userId: number) {
    return this.examRepository.getSessionSnapshot(examId, userId).then((snapshot) => {
      if (!snapshot) {
        return this.config.realtime.autoSubmitLockSeconds;
      }
      return ttlSecondsFromEnd(endTimeOf(snapshot), this.config.realtime.ttlHoursAfterExam);
    });
  }
}
