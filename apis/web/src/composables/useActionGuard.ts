import { ElMessage, ElMessageBox } from 'element-plus';

export function useActionGuard() {
  function ensure(condition: unknown, message: string) {
    if (condition) {
      return true;
    }

    ElMessage.warning(message);
    return false;
  }

  async function confirmAction(
    message: string,
    title = '操作确认',
    options?: {
      confirmButtonText?: string;
      cancelButtonText?: string;
      type?: 'success' | 'warning' | 'info' | 'error';
    }
  ) {
    try {
      await ElMessageBox.confirm(message, title, {
        type: options?.type ?? 'warning',
        confirmButtonText: options?.confirmButtonText ?? '确认',
        cancelButtonText: options?.cancelButtonText ?? '取消'
      });
      return true;
    } catch {
      return false;
    }
  }

  async function confirmDanger(message: string, title = '高风险确认') {
    return confirmAction(message, title, {
      type: 'error',
      confirmButtonText: '继续执行'
    });
  }

  return {
    ensure,
    confirmAction,
    confirmDanger
  };
}
