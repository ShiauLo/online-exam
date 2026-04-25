import type { Pool, RowDataPacket } from 'mysql2/promise';
import type {
  ExamSessionSnapshot,
  PaperQuestionSnapshot,
  ScoreDetailSnapshot,
  ScoreRecordSnapshot
} from '../types.js';

type Row = RowDataPacket & Record<string, unknown>;

function toNumber(value: unknown) {
  return Number(value);
}

function toDate(value: unknown) {
  return value instanceof Date ? value : new Date(String(value));
}

function parseOptions(value: unknown) {
  if (Array.isArray(value)) {
    return value.map((item) => String(item));
  }
  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => String(item));
      }
    } catch (_error) {
      return [];
    }
  }
  return [];
}

export class MysqlExamReadRepository {
  constructor(private readonly pool: Pool) {}

  async getSessionSnapshot(examId: number, studentId: number): Promise<ExamSessionSnapshot | null> {
    const [examRows] = await this.pool.execute<Row[]>(
      `SELECT exam_id, exam_name, status, paper_id, duration, start_time
       FROM exam_instance
       WHERE exam_id = ?`,
      [examId]
    );
    const exam = examRows[0];
    if (!exam) {
      return null;
    }

    const [relationRows] = await this.pool.execute<Row[]>(
      `SELECT relation_id, class_id
       FROM exam_instance_student
       WHERE exam_id = ? AND student_id = ?`,
      [examId, studentId]
    );
    const relation = relationRows[0];
    if (!relation) {
      return null;
    }

    const [scoreRows] = await this.pool.execute<Row[]>(
      `SELECT score_id, class_id, status, submitted_at
       FROM exam_score_record
       WHERE exam_id = ? AND student_id = ?`,
      [examId, studentId]
    );
    const scoreRow = scoreRows[0];
    if (!scoreRow) {
      return null;
    }

    const score: ScoreRecordSnapshot = {
      scoreId: toNumber(scoreRow.score_id),
      classId: toNumber(scoreRow.class_id),
      status: String(scoreRow.status),
      submittedAt: scoreRow.submitted_at ? toDate(scoreRow.submitted_at) : null
    };

    const [detailRows] = await this.pool.execute<Row[]>(
      `SELECT detail_id, question_id, sort_no, question_type, question_stem, student_answer, assigned_score
       FROM exam_score_detail
       WHERE score_id = ?
       ORDER BY sort_no ASC, detail_id ASC`,
      [score.scoreId]
    );

    const scoreDetails: ScoreDetailSnapshot[] = detailRows.map((row) => ({
      detailId: toNumber(row.detail_id),
      questionId: toNumber(row.question_id),
      sortNo: toNumber(row.sort_no),
      questionType: String(row.question_type),
      questionStem: String(row.question_stem),
      studentAnswer: row.student_answer === null ? null : String(row.student_answer),
      assignedScore: toNumber(row.assigned_score)
    }));

    const [paperQuestionRows] = await this.pool.execute<Row[]>(
      `SELECT pq.question_id, pq.sort_no, pq.assigned_score, qi.options
       FROM exam_paper_question pq
       LEFT JOIN question_item qi ON qi.question_id = pq.question_id
       WHERE pq.paper_id = ?
       ORDER BY pq.sort_no ASC, pq.binding_id ASC`,
      [toNumber(exam.paper_id)]
    );

    const paperQuestions: PaperQuestionSnapshot[] = paperQuestionRows.map((row) => ({
      questionId: toNumber(row.question_id),
      sortNo: toNumber(row.sort_no),
      assignedScore: toNumber(row.assigned_score),
      options: parseOptions(row.options)
    }));

    return {
      exam: {
        examId: toNumber(exam.exam_id),
        examName: String(exam.exam_name),
        status: String(exam.status),
        paperId: toNumber(exam.paper_id),
        duration: toNumber(exam.duration),
        startTime: toDate(exam.start_time)
      },
      relation: {
        relationId: toNumber(relation.relation_id),
        classId: toNumber(relation.class_id)
      },
      score,
      scoreDetails,
      paperQuestions
    };
  }
}
