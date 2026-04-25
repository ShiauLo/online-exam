export function readStorage<T>(key: string, fallback: T): T {
  const raw = window.localStorage.getItem(key);
  if (!raw) {
    return fallback;
  }

  try {
    return JSON.parse(raw) as T;
  } catch (error) {
    console.warn(`解析本地存储失败: ${key}`, error);
    return fallback;
  }
}

export function writeStorage<T>(key: string, value: T) {
  window.localStorage.setItem(key, JSON.stringify(value));
}

export function removeStorage(key: string) {
  window.localStorage.removeItem(key);
}
