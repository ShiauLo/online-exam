import { createHash } from 'node:crypto';
import { createAppError, ErrorCatalog } from '../errors.js';
import { isStudent, isTeacher } from '../roles.js';
import type { AppConfig, ReportAbnormalRequest, RequestContext } from '../types.js';
import { endTimeOf, ttlSecondsFromEnd } from './helpers.js';
import { MysqlAbnormalRepository } from '../repositories/mysqlAbnormalRepository.js';
import { MysqlExamReadRepository } from '../repositories/mysqlExamReadRepository.js';
import { RedisRealtimeRepository } from '../repositories/redisRealtimeRepository.js';

function nextId() {
  const raw = `${Date.now()}${Math.floor(Math.random() * 10000)
    .toString()
    .padStart(4, '0')}`;
  return raw;
}

export class DefaultAbnormalService {
  constructor(
    private readonly abnormalRepository: MysqlAbnormalRepository,
    private readonly examRepository: MysqlExamReadRepository,
    private readonly redisRepository: RedisRealtimeRepository,
    private readonly config: AppConfig
  ) {}

  async report(request: ReportAbnormalRequest, context: RequestContext) {
    if (
      context.userId === null ||
      context.roleId === null ||
      (!isStudent(context.roleId) && !isTeacher(context.roleId))
    ) {
      throw createAppError(ErrorCatalog.REALTIME_REPORT_SCREEN_FORBIDDEN);
    }

    const snapshot = await this.examRepository.getSessionSnapshot(request.examId, context.userId);
    if (!snapshot && isStudent(context.roleId)) {
      throw createAppError(ErrorCatalog.EXAM_NOT_FOUND);
    }

    const current = isStudent(context.roleId)
      ? await this.redisRepository.getDraftState(request.examId, context.userId)
      : { answers: {}, screenOutCount: 0 };
    const nextScreenOutCount =
      request.type === 'screen-out'
        ? Math.max(current.screenOutCount, Number(request.screenOutCount ?? current.screenOutCount))
        : current.screenOutCount;

    if (snapshot && isStudent(context.roleId)) {
      await this.redisRepository.saveDraftState(
        request.examId,
        context.userId,
        {
          ...current,
          screenOutCount: nextScreenOutCount
        },
        ttlSecondsFromEnd(endTimeOf(snapshot), this.config.realtime.ttlHoursAfterExam)
      );
    }

    const abnormalId = nextId();
    const now = new Date();
    await this.abnormalRepository.save({
      abnormalId,
      examId: request.examId,
      reporterId: context.userId,
      reporterRoleId: context.roleId,
      type: request.type,
      description: request.desc,
      imgUrlsJson: JSON.stringify(request.imgUrls ?? []),
      screenOutCount: request.type === 'screen-out' ? nextScreenOutCount : null,
      requestId: context.requestId,
      createTime: now
    });

    return {
      abnormalId,
      reportedAt: now.toISOString()
    };
  }
}
