import { createAppError, ErrorCatalog } from '../errors.js';
import { isStudent } from '../roles.js';
import type {
  AppConfig,
  DraftState,
  ExamSessionSnapshot,
  RealtimeSessionRequest,
  RealtimeSessionView,
  RequestContext
} from '../types.js';
import { mergedAnswersOf, normalizeQuestionType, remainSecondsOf, endTimeOf } from './helpers.js';
import { MysqlExamReadRepository } from '../repositories/mysqlExamReadRepository.js';
import { RedisRealtimeRepository } from '../repositories/redisRealtimeRepository.js';

export class DefaultSessionService {
  constructor(
    private readonly examRepository: MysqlExamReadRepository,
    private readonly redisRepository: RedisRealtimeRepository,
    private readonly config: AppConfig
  ) {}

  async loadSession(request: RealtimeSessionRequest, context: RequestContext): Promise<RealtimeSessionView> {
    if (!isStudent(context.roleId) || context.userId === null) {
      throw createAppError(ErrorCatalog.REALTIME_SESSION_FORBIDDEN);
    }

    const snapshot = await this.examRepository.getSessionSnapshot(request.examId, context.userId);
    if (!snapshot) {
      throw createAppError(ErrorCatalog.EXAM_NOT_FOUND);
    }

    if (!['PUBLISHED', 'UNDERWAY'].includes(snapshot.exam.status) || snapshot.exam.startTime > new Date()) {
      throw createAppError(ErrorCatalog.REALTIME_SUBMIT_CONFLICT, '当前考试尚不可进入');
    }

    if (snapshot.score.submittedAt) {
      throw createAppError(ErrorCatalog.REALTIME_SUBMIT_CONFLICT, '当前考试已正式提交');
    }

    const draftState = await this.redisRepository.getDraftState(request.examId, context.userId);
    return this.toView(snapshot, draftState);
  }

  private toView(snapshot: ExamSessionSnapshot, draftState: DraftState): RealtimeSessionView {
    const paperQuestionById = new Map(
      snapshot.paperQuestions.map((item) => [item.questionId, item])
    );
    const answers = mergedAnswersOf(snapshot, draftState);
    const questions = snapshot.scoreDetails.map((detail) => {
      const paperQuestion = paperQuestionById.get(detail.questionId);
      return {
        questionId: detail.questionId,
        stem: detail.questionStem,
        type: normalizeQuestionType(detail.questionType),
        sortNo: detail.sortNo,
        assignedScore: detail.assignedScore,
        options: paperQuestion?.options ?? []
      };
    });
    const endTime = endTimeOf(snapshot);

    return {
      exam: {
        examId: snapshot.exam.examId,
        examName: snapshot.exam.examName,
        status: snapshot.exam.status,
        duration: snapshot.exam.duration,
        remainSeconds: remainSecondsOf(snapshot),
        startTime: snapshot.exam.startTime.toISOString(),
        endTime: endTime.toISOString()
      },
      questions,
      answers,
      currentQId: draftState.currentQId,
      screenOutCount: draftState.screenOutCount
    };
  }
}
