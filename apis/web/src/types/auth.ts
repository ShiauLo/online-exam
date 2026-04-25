export type RoleType =
  | 'student'
  | 'teacher'
  | 'admin'
  | 'super_admin'
  | 'auditor'
  | 'ops';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export interface UserProfile {
  accountId: string;
  username: string;
  realName: string;
  roleType: RoleType;
  phone?: string;
  email?: string;
  avatar?: string;
  className?: string;
  demoPassword?: string;
}

export interface LoginPayload {
  loginType?: 'password_login' | 'one_key_login';
  account?: string;
  password?: string;
  phone?: string;
  verifyCode?: string;
}

export interface LoginResult extends AuthTokens {
  user: UserProfile;
  requireVerifyCode?: boolean;
}
