export interface ApiResponse<T> {
  code: number;
  msg: string;
  data: T;
  requestId: string;
  timestamp: number;
}

export interface ApiErrorPayload {
  code: number;
  msg: string;
  requestId: string;
  timestamp: number;
  errors?: string[];
}

export interface PageQuery {
  pageNum: number;
  pageSize: number;
}

export interface PageResult<T> {
  records?: T[];
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}
