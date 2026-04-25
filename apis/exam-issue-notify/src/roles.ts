export function isAdmin(roleId: number | null | undefined) {
  return roleId === 1 || roleId === 2;
}

export function isTeacher(roleId: number | null | undefined) {
  return roleId === 3;
}

export function isStudent(roleId: number | null | undefined) {
  return roleId === 4;
}

export function isAuditor(roleId: number | null | undefined) {
  return roleId === 5;
}

export function isOps(roleId: number | null | undefined) {
  return roleId === 6;
}

export function canUseIssueNotify(roleId: number | null | undefined) {
  return isAdmin(roleId) || isTeacher(roleId) || isStudent(roleId) || isAuditor(roleId) || isOps(roleId);
}
