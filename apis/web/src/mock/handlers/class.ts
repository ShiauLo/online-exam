import type { ApiResponse } from '@/types/api';
import { mockClasses, mockUsers } from '../data';
import {
  applyQueryFilter,
  fail,
  paginate,
  requireCurrentUser,
  success,
  type MockRequestContext
} from './shared';

export function handleClassMock(context: MockRequestContext): ApiResponse<unknown> | null {
  const { method, url, data, token } = context;

  if (url === '/api/class/query' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    let list = [...mockClasses];

    if (currentUser.roleType === 'teacher') {
      list = list.filter((item) => item.teacherId === currentUser.accountId);
    }

    if (currentUser.roleType === 'student') {
      list = list.filter(
        (item) =>
          item.students.includes(currentUser.accountId) ||
          item.pendingStudents.includes(currentUser.accountId)
      );
    }

    list = applyQueryFilter(list, String(data.keyword ?? ''), (item) => [
      item.className,
      item.description,
      item.teacherName
    ]);

    return success(
      paginate(list, Number(data.pageNum ?? 1), Number(data.pageSize ?? 10))
    );
  }

  if (url === '/api/class/create' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    if (currentUser.roleType === 'teacher') {
      const ownClassCount = mockClasses.filter(
        (item) => item.teacherId === currentUser.accountId
      ).length;
      if (ownClassCount >= 1) {
        fail(409, '教师默认仅可创建 1 个专属班级');
      }
    }

    const record = {
      classId: `c-${mockClasses.length + 100}`,
      className: String(data.className ?? '未命名班级'),
      description: String(data.description ?? ''),
      teacherId: String(data.teacherId ?? currentUser.accountId),
      teacherName:
        mockUsers.find(
          (item) => item.accountId === String(data.teacherId ?? currentUser.accountId)
        )?.realName ?? currentUser.realName,
      classCode: `AUTO${mockClasses.length + 100}`,
      approvedMemberCount: 0,
      pendingMemberCount: 0,
      isMandatory: false,
      createdBy: currentUser.username,
      createTime: new Date()
        .toLocaleString('zh-CN', { hour12: false })
        .replace(/\//g, '-'),
      updateTime: new Date()
        .toLocaleString('zh-CN', { hour12: false })
        .replace(/\//g, '-'),
      students: [],
      pendingStudents: []
    };

    mockClasses.unshift(record);
    return success(record);
  }

  if (url === '/api/class/update' && method === 'PUT') {
    const record = mockClasses.find((item) => item.classId === String(data.classId ?? ''));
    if (!record) {
      fail(404, '班级不存在');
    }
    record.className = String(data.className ?? record.className);
    record.description = String(data.description ?? record.description);
    record.updateTime = new Date()
      .toLocaleString('zh-CN', { hour12: false })
      .replace(/\//g, '-');
    return success(record);
  }

  if (url === '/api/class/delete' && method === 'DELETE') {
    const index = mockClasses.findIndex((item) => item.classId === String(data.classId ?? ''));
    if (index < 0) {
      fail(404, '班级不存在');
    }
    mockClasses.splice(index, 1);
    return success(true);
  }

  if (url === '/api/class/apply-join' && method === 'POST') {
    const currentUser = requireCurrentUser(token);
    const record = mockClasses.find((item) => item.classCode === String(data.classCode ?? ''));
    if (!record) {
      fail(404, '班级码不存在');
    }
    if (!record.pendingStudents.includes(currentUser.accountId)) {
      record.pendingStudents.push(currentUser.accountId);
      record.pendingMemberCount = record.pendingStudents.length;
    }
    return success(true);
  }

  if (url === '/api/class/approve-join' && method === 'PUT') {
    const record = mockClasses.find((item) => item.classId === String(data.classId ?? ''));
    if (!record) {
      fail(404, '班级不存在');
    }
    const studentIds = Array.isArray(data.studentIds)
      ? (data.studentIds as string[])
      : [String(data.studentId ?? '')];
    studentIds.forEach((studentId) => {
      record.pendingStudents = record.pendingStudents.filter((item) => item !== studentId);
      if (!record.students.includes(studentId)) {
        record.students.push(studentId);
      }
    });
    record.approvedMemberCount = record.students.length;
    record.pendingMemberCount = record.pendingStudents.length;
    return success(true);
  }

  if (url === '/api/class/remove-student' && method === 'PUT') {
    const record = mockClasses.find((item) => item.classId === String(data.classId ?? ''));
    if (!record) {
      fail(404, '班级不存在');
    }
    record.students = record.students.filter((item) => item !== String(data.studentId ?? ''));
    record.approvedMemberCount = record.students.length;
    return success(true);
  }

  if (url === '/api/class/quit' && method === 'PUT') {
    const currentUser = requireCurrentUser(token);
    const record = mockClasses.find((item) => item.classId === String(data.classId ?? ''));
    if (!record) {
      fail(404, '班级不存在');
    }
    if (record.isMandatory) {
      fail(409, '强制班级不可自行退出');
    }
    record.students = record.students.filter((item) => item !== currentUser.accountId);
    record.approvedMemberCount = record.students.length;
    return success(true);
  }

  if (url === '/api/class/import' && method === 'POST') {
    return success({ totalCount: 2, successCount: 2, failedCount: 0 });
  }

  return null;
}
