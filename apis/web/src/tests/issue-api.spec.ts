import { beforeEach, describe, expect, it, vi } from 'vitest';

const requestMock = vi.fn();
const emitMockIssueEvent = vi.fn();

vi.mock('@/api/http', () => ({
  request: requestMock
}));

vi.mock('@/api/socket', () => ({
  emitMockIssueEvent
}));

describe('issue API 适配', () => {
  beforeEach(() => {
    requestMock.mockReset();
    emitMockIssueEvent.mockReset();
  });

  it('trackIssue 会把真实轨迹视图转换成时间线数组', async () => {
    requestMock.mockResolvedValue({
      issueId: '1001',
      logs: [
        {
          action: 'CREATED',
          content: '创建问题',
          occurredAt: '2026-04-24T08:00:00'
        },
        {
          action: 'HANDLED',
          content: '教师已接单',
          occurredAt: '2026-04-24T08:05:00'
        }
      ]
    });

    const { trackIssue } = await import('@/api/issue');
    const result = await trackIssue({ issueId: '1001' });

    expect(result).toEqual([
      {
        time: '2026-04-24T08:00:00',
        title: '问题已创建',
        desc: '创建问题'
      },
      {
        time: '2026-04-24T08:05:00',
        title: '问题已处理',
        desc: '教师已接单'
      }
    ]);
  });

  it('closeIssue 会补齐 confirmResult 默认值', async () => {
    requestMock.mockResolvedValue({
      issueId: '1001',
      status: 'CLOSED'
    });

    const { closeIssue } = await import('@/api/issue');
    await closeIssue({
      issueId: '1001',
      comment: '前端关闭'
    });

    expect(requestMock).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/api/issue/core/close',
        method: 'PUT',
        data: expect.objectContaining({
          issueId: '1001',
          comment: '前端关闭',
          confirmResult: 'CONFIRMED'
        })
      })
    );
  });
});
