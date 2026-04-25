import { ref } from 'vue';

function resolveErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'string' && error.trim()) {
    return error;
  }

  const candidate =
    (error as { response?: { data?: { msg?: string } }; message?: string })?.response?.data?.msg ||
    (error as { message?: string })?.message;

  return candidate && String(candidate).trim() ? String(candidate) : fallback;
}

export function useLoadFeedback() {
  const loading = ref(false);
  const loadError = ref('');

  async function runLoad(task: () => Promise<void>, fallback = '数据加载失败，请稍后重试') {
    loading.value = true;
    loadError.value = '';
    try {
      await task();
    } catch (error) {
      loadError.value = resolveErrorMessage(error, fallback);
    } finally {
      loading.value = false;
    }
  }

  function clearLoadError() {
    loadError.value = '';
  }

  return {
    loading,
    loadError,
    runLoad,
    clearLoadError
  };
}
