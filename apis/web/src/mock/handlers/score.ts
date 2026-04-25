import type { ApiResponse } from '@/types/api';
import { mockScores } from '../data';
import { fail, paginate, requireCurrentUser, success, type MockRequestContext } from './shared';

export function handleScoreMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/score/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockScores];
    if (currentUser.roleType === 'student') {
      list = list.filter((item) => item.studentId === currentUser.accountId);
    }
    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.classId === 'c-101');
    }
    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/score/detail' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const record = mockScores.find(
      (item) =>
        item.examId === String(data.examId ?? '') &&
        item.studentId === String(data.studentId ?? currentUser.accountId)
    );
    if (!record) {
      fail(404, '成绩明细不存在');
    }
    return success(record);
  }

  if (url === '/api/score/apply-recheck' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const score = mockScores.find((item) => item.studentId === currentUser.accountId);
    if (score) {
      score.recheckStatus = 'pending';
    }
    return success(true);
  }

  if (url === '/api/score/manual-score' && method === 'PUT') {
    const score = mockScores.find(
      (item) =>
        item.examId === String(data.examId ?? '') &&
        item.studentId === String(data.studentId ?? '')
    );
    if (!score) {
      fail(404, '成绩不存在');
    }
    score.subjectiveScore = Number(data.score ?? score.subjectiveScore);
    score.totalScore = score.objectiveScore + score.subjectiveScore;
    return success(score);
  }

  if (url === '/api/score/publish' && method === 'PUT') {
    mockScores.forEach((item) => {
      if (item.examId === String(data.examId ?? item.examId)) {
        item.publishStatus = 'published';
      }
    });
    return success(true);
  }

  if (url === '/api/score/analyze' && method === 'POST') {
    const scores = mockScores.map((item) => item.totalScore);
    const average = scores.reduce((sum, value) => sum + value, 0) / scores.length;
    const passRate = scores.filter((item) => item >= 60).length / scores.length;

    return success({
      average: Number(average.toFixed(1)),
      passRate: Number((passRate * 100).toFixed(1)),
      distribution: [
        { label: '90+', value: scores.filter((item) => item >= 90).length },
        { label: '80-89', value: scores.filter((item) => item >= 80 && item < 90).length },
        { label: '60-79', value: scores.filter((item) => item >= 60 && item < 80).length },
        { label: '<60', value: scores.filter((item) => item < 60).length }
      ]
    });
  }

  if (url === '/api/score/export' && method === 'GET') {
    return success({ fileKey: 'score-export-001' });
  }

  if (url === '/api/score/handle-appeal' && method === 'PUT') {
    const score = mockScores.find((item) => item.scoreId === String(data.appealId ?? 's-1'));
    if (score) {
      score.recheckStatus = 'processed';
    }
    return success(true);
  }

  return null;
}
