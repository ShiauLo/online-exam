import type { Pool, RowDataPacket } from 'mysql2/promise';
import type { IssueCreatedRow, IssueNotifyRepository, IssueProcessRow } from '../types.js';

interface CreatedIssueRowPacket extends RowDataPacket {
  issueId: string | number | bigint;
  type: string;
  title: string;
  status: string;
  reporterId: string | number | bigint;
  reporterName?: string;
  handlerId: string | number | bigint | null;
  handlerName?: string;
  teacherIds?: string | null;
  description?: string;
  occurredAt: Date | string;
}

interface ProcessIssueRowPacket extends RowDataPacket {
  logId: string | number | bigint;
  issueId: string | number | bigint;
  type: string;
  title: string;
  status: string;
  reporterId: string | number | bigint;
  reporterName?: string;
  handlerId: string | number | bigint | null;
  handlerName?: string;
  teacherIds?: string | null;
  action: string;
  content?: string;
  occurredAt: Date | string;
}

export class MysqlIssueNotifyRepository implements IssueNotifyRepository {
  constructor(private readonly pool: Pool) {}

  private parseTeacherIds(raw: string | null | undefined) {
    if (!raw) {
      return [] as number[];
    }
    return raw
      .split(',')
      .map((item) => Number(item.trim()))
      .filter((item) => Number.isInteger(item) && item > 0);
  }

  private stringifyId(raw: string | number | bigint) {
    return String(raw);
  }

  private toNumber(raw: string | number | bigint) {
    return Number(raw);
  }

  private toNullableNumber(raw: string | number | bigint | null | undefined) {
    if (raw === null || raw === undefined) {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  async listCreatedIssuesSince(since: Date, limit: number, preferLatest = false): Promise<IssueCreatedRow[]> {
    const orderDirection = preferLatest ? 'DESC' : 'ASC';
    const [rows] = await this.pool.query<CreatedIssueRowPacket[]>(
      `SELECT
          r.issue_id AS issueId,
          r.type AS type,
          r.title AS title,
          r.status AS status,
          r.reporter_id AS reporterId,
          reporter.real_name AS reporterName,
          r.current_handler_id AS handlerId,
          handler.real_name AS handlerName,
          COALESCE(CAST(direct_class.teacher_id AS CHAR), exam_scope.teacher_ids) AS teacherIds,
          r.description AS description,
          r.create_time AS occurredAt
        FROM exam_issue_record r
        LEFT JOIN sys_user reporter ON reporter.id = r.reporter_id
        LEFT JOIN sys_user handler ON handler.id = r.current_handler_id
        LEFT JOIN exam_class direct_class ON direct_class.class_id = r.class_id
        LEFT JOIN (
          SELECT
            eic.exam_id,
            GROUP_CONCAT(DISTINCT ec.teacher_id ORDER BY ec.teacher_id SEPARATOR ',') AS teacher_ids
          FROM exam_instance_class eic
          LEFT JOIN exam_class ec ON ec.class_id = eic.class_id
          GROUP BY eic.exam_id
        ) exam_scope ON exam_scope.exam_id = r.exam_id
        WHERE r.create_time >= ?
        ORDER BY r.create_time ${orderDirection}, r.issue_id ${orderDirection}
        LIMIT ?`,
      [since, limit]
    );

    return rows.map((row) => ({
      issueId: this.stringifyId(row.issueId),
      type: row.type,
      title: row.title,
      status: row.status,
      reporterId: this.toNumber(row.reporterId),
      reporterName: row.reporterName ?? undefined,
      handlerId: this.toNullableNumber(row.handlerId),
      handlerName: row.handlerName ?? undefined,
      teacherIds: this.parseTeacherIds(row.teacherIds),
      description: row.description ?? undefined,
      occurredAt: row.occurredAt
    }));
  }

  async listProcessUpdatesSince(since: Date, limit: number, preferLatest = false): Promise<IssueProcessRow[]> {
    const orderDirection = preferLatest ? 'DESC' : 'ASC';
    const [rows] = await this.pool.query<ProcessIssueRowPacket[]>(
      `SELECT
          l.log_id AS logId,
          l.issue_id AS issueId,
          r.type AS type,
          r.title AS title,
          r.status AS status,
          r.reporter_id AS reporterId,
          reporter.real_name AS reporterName,
          r.current_handler_id AS handlerId,
          handler.real_name AS handlerName,
          COALESCE(CAST(direct_class.teacher_id AS CHAR), exam_scope.teacher_ids) AS teacherIds,
          l.action AS action,
          l.content AS content,
          l.create_time AS occurredAt
        FROM exam_issue_process_log l
        INNER JOIN exam_issue_record r ON r.issue_id = l.issue_id
        LEFT JOIN sys_user reporter ON reporter.id = r.reporter_id
        LEFT JOIN sys_user handler ON handler.id = r.current_handler_id
        LEFT JOIN exam_class direct_class ON direct_class.class_id = r.class_id
        LEFT JOIN (
          SELECT
            eic.exam_id,
            GROUP_CONCAT(DISTINCT ec.teacher_id ORDER BY ec.teacher_id SEPARATOR ',') AS teacher_ids
          FROM exam_instance_class eic
          LEFT JOIN exam_class ec ON ec.class_id = eic.class_id
          GROUP BY eic.exam_id
        ) exam_scope ON exam_scope.exam_id = r.exam_id
        WHERE l.create_time >= ?
          AND l.action IN ('HANDLED', 'TRANSFERRED', 'CLOSED')
        ORDER BY l.create_time ${orderDirection}, l.log_id ${orderDirection}
        LIMIT ?`,
      [since, limit]
    );

    return rows.map((row) => ({
      logId: this.stringifyId(row.logId),
      issueId: this.stringifyId(row.issueId),
      type: row.type,
      title: row.title,
      status: row.status,
      reporterId: this.toNumber(row.reporterId),
      reporterName: row.reporterName ?? undefined,
      handlerId: this.toNullableNumber(row.handlerId),
      handlerName: row.handlerName ?? undefined,
      teacherIds: this.parseTeacherIds(row.teacherIds),
      action: row.action,
      content: row.content ?? undefined,
      occurredAt: row.occurredAt
    }));
  }
}
