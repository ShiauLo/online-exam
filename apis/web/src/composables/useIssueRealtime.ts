import { onBeforeUnmount, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { trackIssue } from '@/api/issue';
import { createIssueSocket } from '@/api/socket';

type IssueRow = Record<string, any>;
type IssueSocketPayload = Record<string, unknown>;

interface IssueSocketLike {
  on: (event: string, callback: (payload: IssueSocketPayload) => void) => void;
  off: (event: string, callback?: (payload: IssueSocketPayload) => void) => void;
  emit: (event: string, payload: Record<string, unknown>) => void;
  disconnect: () => void;
}

function buildNotice(payload: IssueSocketPayload) {
  const title = String(payload.title ?? '问题单');
  const processDesc = String(payload.processDesc ?? '').trim();
  const action = String(payload.action ?? 'updated');
  const prefix =
    action === 'created'
      ? '新问题'
      : action === 'handled'
        ? '已接单'
        : action === 'transferred'
          ? '已转派'
          : action === 'closed'
            ? '已关闭'
            : '流程更新';
  return processDesc ? `${prefix}：${title}，${processDesc}` : `${prefix}：${title}`;
}

function noticeType(payload: IssueSocketPayload) {
  const action = String(payload.action ?? 'updated');
  if (action === 'closed' || action === 'handled') {
    return 'success' as const;
  }
  if (action === 'transferred') {
    return 'warning' as const;
  }
  return 'info' as const;
}

export function useIssueRealtime(loadIssues: () => Promise<void>) {
  const timeline = ref<IssueRow[]>([]);
  const selectedIssueId = ref('');
  const latestMessage = ref('');
  const realtimeConnected = ref(false);
  const realtimeStatusText = ref('实时通知未连接');
  const realtimeStatusClass = ref('soft-tag--warning');
  let issueSocket: IssueSocketLike | null = null;
  let hasConnectedOnce = false;
  let restorePending = false;

  function setRealtimeStatus(text: string, className: string) {
    realtimeStatusText.value = text;
    realtimeStatusClass.value = className;
  }

  async function restoreIssueState() {
    await loadIssues();
    if (selectedIssueId.value) {
      await refreshTimeline(selectedIssueId.value);
    }
  }

  async function refreshTimeline(issueId = selectedIssueId.value) {
    if (!issueId) {
      return;
    }
    const result = await trackIssue({ issueId });
    timeline.value = Array.isArray(result) ? result : [];
  }

  async function previewIssue(row: IssueRow) {
    selectedIssueId.value = String(row.issueId ?? '');
    await refreshTimeline(selectedIssueId.value);
  }

  async function handleRealtime(payload: IssueSocketPayload) {
    latestMessage.value = buildNotice(payload);
    await loadIssues();
    if (selectedIssueId.value && String(payload.issueId ?? '') === selectedIssueId.value) {
      await refreshTimeline(selectedIssueId.value);
    }
    ElMessage({
      type: noticeType(payload),
      message: latestMessage.value
    });
  }

  const handleConnected = () => {
    realtimeConnected.value = true;
    setRealtimeStatus('实时通知已连接', 'soft-tag--success');
    if (restorePending) {
      restorePending = false;
      restoreIssueState()
        .then(() => {
          ElMessage.success('实时通知已恢复，问题数据已同步');
        })
        .catch(() => {
          ElMessage.warning('实时通知已恢复，但问题数据同步失败，请手动刷新');
        });
    }
    hasConnectedOnce = true;
  };

  const handleDisconnect = () => {
    realtimeConnected.value = false;
    restorePending = hasConnectedOnce;
    setRealtimeStatus('实时通知已断开，正在尝试重连', 'soft-tag--warning');
    if (hasConnectedOnce) {
      ElMessage.warning('实时通知已断开，正在尝试重连');
    }
  };

  const handleReconnect = () => {
    restorePending = true;
    realtimeConnected.value = false;
    setRealtimeStatus('实时通知恢复中，正在同步数据', 'soft-tag--warning');
  };

  const handleConnectError = (payload: IssueSocketPayload) => {
    realtimeConnected.value = false;
    restorePending = true;
    const message = String(payload.message ?? '实时通知连接异常，正在尝试恢复');
    setRealtimeStatus(message, 'soft-tag--danger');
    ElMessage.warning(message);
  };

  const handleIssueNotify = (payload: IssueSocketPayload) => {
    handleRealtime(payload).catch(() => undefined);
  };

  const handleProcessNotify = (payload: IssueSocketPayload) => {
    handleRealtime(payload).catch(() => undefined);
  };

  function connectIssueRealtime() {
    setRealtimeStatus('实时通知连接中', 'soft-tag--warning');
    issueSocket = createIssueSocket() as unknown as IssueSocketLike;
    issueSocket.on('connect', handleConnected);
    issueSocket.on('connected', handleConnected);
    issueSocket.on('disconnect', handleDisconnect);
    issueSocket.on('reconnect', handleReconnect);
    issueSocket.on('connect_error', handleConnectError);
    issueSocket.on('issueNotify', handleIssueNotify);
    issueSocket.on('processNotify', handleProcessNotify);
    issueSocket.emit('subscribeIssue', { scope: 'issue' });
  }

  function disconnectIssueRealtime() {
    realtimeConnected.value = false;
    hasConnectedOnce = false;
    restorePending = false;
    setRealtimeStatus('实时通知未连接', 'soft-tag--warning');
    issueSocket?.off('connect', handleConnected);
    issueSocket?.off('connected', handleConnected);
    issueSocket?.off('disconnect', handleDisconnect);
    issueSocket?.off('reconnect', handleReconnect);
    issueSocket?.off('connect_error', handleConnectError);
    issueSocket?.off('issueNotify', handleIssueNotify);
    issueSocket?.off('processNotify', handleProcessNotify);
    issueSocket?.disconnect();
    issueSocket = null;
  }

  onBeforeUnmount(() => {
    disconnectIssueRealtime();
  });

  return {
    timeline,
    latestMessage,
    realtimeConnected,
    realtimeStatusText,
    realtimeStatusClass,
    previewIssue,
    refreshTimeline,
    connectIssueRealtime,
    disconnectIssueRealtime
  };
}
