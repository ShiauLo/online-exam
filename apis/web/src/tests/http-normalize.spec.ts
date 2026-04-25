import { describe, expect, it } from 'vitest';
import { normalizeResponseData } from '@/api/http';

describe('HTTP 响应归一化', () => {
  it('会把 records 兼容映射为 list', () => {
    const normalized = normalizeResponseData({
      records: [{ id: '1' }, { id: '2' }],
      total: 2,
      pageNum: 1,
      pageSize: 10
    }) as {
      records: Array<{ id: string }>;
      list: Array<{ id: string }>;
      total: number;
      pageNum: number;
      pageSize: number;
    };

    expect(normalized.list).toHaveLength(2);
    expect(normalized.list[0].id).toBe('1');
  });

  it('已有 list 时保持原结构不变', () => {
    const payload = {
      list: [{ id: '3' }],
      total: 1,
      pageNum: 1,
      pageSize: 10
    };

    expect(normalizeResponseData(payload)).toEqual(payload);
  });
});
