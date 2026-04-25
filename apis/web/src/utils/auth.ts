import type { AuthTokens, UserProfile } from '@/types/auth';
import { readStorage, removeStorage, writeStorage } from './storage';

const TOKEN_KEY = 'exam-web-tokens';
const USER_KEY = 'exam-web-user';

export function getStoredTokens(): AuthTokens | null {
  return readStorage<AuthTokens | null>(TOKEN_KEY, null);
}

export function setStoredTokens(tokens: AuthTokens) {
  writeStorage(TOKEN_KEY, tokens);
}

export function clearStoredTokens() {
  removeStorage(TOKEN_KEY);
}

export function getStoredUser(): UserProfile | null {
  return readStorage<UserProfile | null>(USER_KEY, null);
}

export function setStoredUser(user: UserProfile) {
  writeStorage(USER_KEY, user);
}

export function clearStoredUser() {
  removeStorage(USER_KEY);
}
