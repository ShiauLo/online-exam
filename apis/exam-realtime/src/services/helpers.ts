import type { DraftState, ExamSessionSnapshot } from '../types.js';

export function normalizeQuestionType(type: string) {
  return type.toLowerCase();
}

export function endTimeOf(snapshot: ExamSessionSnapshot) {
  return new Date(snapshot.exam.startTime.getTime() + snapshot.exam.duration * 60 * 1000);
}

export function remainSecondsOf(snapshot: ExamSessionSnapshot, now = new Date()) {
  const remain = Math.floor((endTimeOf(snapshot).getTime() - now.getTime()) / 1000);
  return Math.max(remain, 0);
}

export function ttlSecondsFromEnd(endTime: Date, ttlHoursAfterExam: number, now = new Date()) {
  const expireAt = endTime.getTime() + ttlHoursAfterExam * 60 * 60 * 1000;
  const remain = Math.ceil((expireAt - now.getTime()) / 1000);
  return Math.max(remain, 60);
}

export function mergedAnswersOf(snapshot: ExamSessionSnapshot, draftState: DraftState) {
  const answersFromScore = snapshot.scoreDetails.reduce<Record<string, string>>((acc, detail) => {
    if (detail.studentAnswer && detail.studentAnswer.trim()) {
      acc[String(detail.questionId)] = detail.studentAnswer;
    }
    return acc;
  }, {});

  return {
    ...answersFromScore,
    ...(draftState.answers ?? {})
  };
}
