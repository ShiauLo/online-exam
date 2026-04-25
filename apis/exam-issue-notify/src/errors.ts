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
  AUTHENTICATION_REQUIRED: { httpStatus: 401, code: 1001, message: '未登录或登录态失效' },
  BAD_REQUEST: { httpStatus: 400, code: 4000, message: '参数错误' },
  ISSUE_NOTIFY_FORBIDDEN: { httpStatus: 403, code: 2001, message: '无权限查看问题实时通知' },
  ISSUE_NOTIFY_SUBSCRIBE_FORBIDDEN: { httpStatus: 403, code: 2002, message: '无权限订阅问题实时通知' },
  ISSUE_NOTIFY_CURSOR_INVALID: { httpStatus: 400, code: 2003, message: '通知游标格式不正确' }
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
