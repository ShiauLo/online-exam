import { handleAccountMock } from './handlers/account';
import { handleClassMock } from './handlers/class';
import { handleExamMock } from './handlers/exam';
import { handleIssueMock } from './handlers/issue';
import { handlePaperMock } from './handlers/paper';
import { handleQuestionMock } from './handlers/question';
import { handleScoreMock } from './handlers/score';
import { handleSystemMock } from './handlers/system';
import { buildContext, fail, type MockRequestConfig } from './handlers/shared';

const handlers = [
  handleAccountMock,
  handleClassMock,
  handleQuestionMock,
  handlePaperMock,
  handleExamMock,
  handleScoreMock,
  handleSystemMock,
  handleIssueMock
];

export async function handleMockRequest<T = unknown>(config: MockRequestConfig) {
  const context = buildContext(config);
  await new Promise((resolve) => setTimeout(resolve, 120));

  if (context.url === '/api/resource/file/download' && context.method === 'GET') {
    return {
      code: 200,
      msg: 'success',
      data: { fileKey: String(context.params.fileKey ?? 'download-file') },
      requestId: `req-${Math.random().toString(36).slice(2, 10)}`,
      timestamp: Date.now()
    } as unknown as Awaited<ReturnType<(typeof handlers)[number]>>;
  }

  for (const handler of handlers) {
    const result = handler(context);
    if (result) {
      return result as Awaited<ReturnType<typeof handler>>;
    }
  }

  fail(404, `未实现的 Mock 接口: ${context.method} ${context.url}`);
}

export { MockHttpError, type MockRequestConfig } from './handlers/shared';
