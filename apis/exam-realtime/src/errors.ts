export class AppError extends Error {
  readonly httpStatus: number;
  readonly domainCode: number;
  readonly errors?: string[];

  constructor(httpStatus: number, domainCode: number, message: string, errors?: string[]) {
    super(message);
    this.httpStatus = httpStatus;
    this.domainCode = domainCode;
    this.errors = errors;
  }
}

export const ErrorCatalog = {
  EXAM_NOT_FOUND: { httpStatus: 404, code: 1501, message: '考试不存在' },
  SCORE_NOT_FOUND: { httpStatus: 404, code: 1601, message: '成绩不存在' },
  REALTIME_SESSION_FORBIDDEN: { httpStatus: 403, code: 1901, message: '无权限进入当前考试会话' },
  REALTIME_PROGRESS_FORBIDDEN: { httpStatus: 403, code: 1902, message: '无权限保存当前考试进度' },
  REALTIME_SOCKET_FORBIDDEN: { httpStatus: 403, code: 1903, message: '无权限建立当前考试实时连接' },
  REALTIME_REPORT_SCREEN_FORBIDDEN: { httpStatus: 403, code: 1904, message: '无权限上报切屏事件' },
  REALTIME_SUBMIT_CONFLICT: { httpStatus: 409, code: 1905, message: '当前考试状态不允许提交' },
  REALTIME_DOWNSTREAM_ERROR: { httpStatus: 500, code: 1906, message: '下游考试核心服务调用失败' },
  ISSUE_NOTIFY_FORBIDDEN: { httpStatus: 403, code: 2001, message: '无权限查看问题实时通知' },
  ISSUE_NOTIFY_SUBSCRIBE_FORBIDDEN: { httpStatus: 403, code: 2002, message: '无权限订阅问题实时通知' },
  ISSUE_NOTIFY_CURSOR_INVALID: { httpStatus: 400, code: 2003, message: '通知游标格式不正确' },
  AUTHENTICATION_REQUIRED: { httpStatus: 401, code: 1001, message: '未登录或登录态失效' },
  BAD_REQUEST: { httpStatus: 400, code: 4000, message: '参数错误' }
} as const;

export function createAppError(
  error: {
    httpStatus: number;
    code: number;
    message: string;
  },
  message?: string,
  errors?: string[]
) {
  return new AppError(error.httpStatus, error.code, message ?? error.message, errors);
}
