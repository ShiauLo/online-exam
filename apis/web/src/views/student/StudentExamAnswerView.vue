<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag soft-tag--warning">学生答题页</span>
        <h1 class="page-hero__title">{{ examStore.examName || '考试答题' }}</h1>
        <p class="page-hero__desc">
          已接入倒计时、自动保存、断线提示和切屏上报。离开页面前请先保存或提交。
        </p>
      </div>
      <div class="hero-actions">
        <span class="soft-tag" :class="connectionStatusClass">{{ connectionStatusText }}</span>
        <span class="soft-tag soft-tag--warning">剩余 {{ examStore.remainLabel }}</span>
        <AuthorizedAction permission="exam.saveProgress" @click="saveProgress" :loading="saving">手动保存</AuthorizedAction>
        <AuthorizedAction permission="exam.submit" type="danger" @click="submitPaper" :loading="submitting">提交试卷</AuthorizedAction>
      </div>
    </section>

    <MetricsOverview :items="metrics" />

    <section class="page-grid">
      <ExamQuestionCard
        v-for="(question, index) in examStore.questions"
        :key="String(question.questionId)"
        :index="index"
        :question="question"
        :single-answer="String(examStore.answers[String(question.questionId)] ?? '')"
        :multi-answer="multiAnswers[String(question.questionId)] ?? []"
        @update-answer="handleQuestionAnswerUpdate"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AuthorizedAction from '@/components/AuthorizedAction.vue';
import ExamQuestionCard from '@/components/ExamQuestionCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import { useExamStore } from '@/stores/exam';
import { createExamSocket } from '@/api/socket';
import { reportExamAbnormal } from '@/api/examRealtime';

interface SocketLike {
  on: (event: string, callback: (payload: any) => void) => void;
  off: (event: string, callback?: (payload: any) => void) => void;
  emit: (event: string, payload: Record<string, unknown>) => void;
  disconnect: () => void;
}

const route = useRoute();
const router = useRouter();
const examStore = useExamStore();
const multiAnswers = reactive<Record<string, string[]>>({});
const state = reactive({
  saving: false,
  submitting: false,
  connectionStatusText: '实时连接准备中',
  connectionStatusClass: 'soft-tag--warning',
  recoveringConnection: false
});

let examSocket: SocketLike | null = null;
let autoSaveTimer: number | null = null;
let autoSubmitted = false;
let hasConnectedOnce = false;
let restorePending = false;

const saving = computed(() => state.saving);
const submitting = computed(() => state.submitting);
const connectionStatusText = computed(() => state.connectionStatusText);
const connectionStatusClass = computed(() => state.connectionStatusClass);
const answeredCount = computed(() => Object.values(examStore.answers).filter((value) => String(value).trim()).length);
const metrics = computed(() => [
  { label: '题目数量', value: examStore.questions.length },
  { label: '已作答数量', value: answeredCount.value },
  { label: '切屏次数', value: examStore.screenOutCount }
]);
const examId = computed(() => {
  const value = route.query.examId;
  if (Array.isArray(value)) {
    return value[0] ?? 'e-1';
  }
  return value ?? 'e-1';
});

function setAnswer(questionId: string, value: string) {
  examStore.setAnswer(questionId, value);
}

function setMultiAnswer(questionId: string, value: string[]) {
  multiAnswers[questionId] = value;
  examStore.setAnswer(questionId, value.join('、'));
}

function handleQuestionAnswerUpdate(payload: {
  questionId: string;
  type: 'single' | 'multi' | 'subjective';
  value: string | string[];
}) {
  if (payload.type === 'multi') {
    setMultiAnswer(payload.questionId, Array.isArray(payload.value) ? payload.value : []);
    return;
  }
  setAnswer(payload.questionId, String(payload.value));
}

function setConnectionStatus(text: string, className: string) {
  state.connectionStatusText = text;
  state.connectionStatusClass = className;
}

function syncExamChannel() {
  examSocket?.emit('enterExam', {
    examId: examId.value,
    duration: Math.max(1, Math.ceil(examStore.remainSeconds / 60)),
    remainSeconds: examStore.remainSeconds
  });
}

async function saveProgress(showMessage = true) {
  state.saving = true;
  try {
    await examStore.persistProgress();
    if (showMessage) {
      ElMessage.success('答题进度已保存');
    }
  } finally {
    state.saving = false;
  }
}

async function submitPaper() {
  state.submitting = true;
  try {
    await ElMessageBox.confirm('提交后将无法继续修改答案，是否继续？', '提交确认', {
      confirmButtonText: '确认提交',
      cancelButtonText: '继续作答',
      type: 'warning'
    });
    await examStore.submit();
    ElMessage.success('试卷已提交');
    router.replace('/student/exam');
  } finally {
    state.submitting = false;
  }
}

async function reportScreenOut() {
  examStore.increaseScreenOut();
  await reportExamAbnormal({
    examId: examId.value,
    type: 'screen-out',
    desc: `检测到切屏 ${examStore.screenOutCount} 次`,
    screenOutCount: examStore.screenOutCount
  });
  examSocket?.emit('reportScreen', {
    examId: examId.value,
    screenOutCount: examStore.screenOutCount
  });
}

function handleVisibilityChange() {
  if (document.hidden) {
    reportScreenOut().catch(() => undefined);
  }
}

function handleSocketConnected() {
  examStore.setConnected(true);
  hasConnectedOnce = true;

  if (restorePending) {
    restorePending = false;
    state.recoveringConnection = true;
    setConnectionStatus('实时连接恢复中，正在同步考试状态', 'soft-tag--warning');
    syncExamChannel();
    return;
  }

  setConnectionStatus('实时连接正常', 'soft-tag--success');
  if (state.recoveringConnection) {
    state.recoveringConnection = false;
    ElMessage.success('实时连接已恢复，考试状态已同步');
  }
}

function handleSocketDisconnect() {
  examStore.setConnected(false);
  restorePending = hasConnectedOnce;
  state.recoveringConnection = hasConnectedOnce;
  setConnectionStatus('实时连接已断开，正在尝试恢复', 'soft-tag--warning');
  if (hasConnectedOnce) {
    ElMessage.warning('实时连接已断开，系统将自动恢复考试状态');
  }
}

function handleSocketReconnect() {
  restorePending = true;
  state.recoveringConnection = true;
  setConnectionStatus('实时连接恢复中，正在同步考试状态', 'soft-tag--warning');
}

function handleSocketConnectError(payload: Record<string, unknown>) {
  examStore.setConnected(false);
  restorePending = true;
  state.recoveringConnection = true;
  const message = String(payload.message ?? '实时连接异常，正在尝试恢复');
  setConnectionStatus(message, 'soft-tag--danger');
  ElMessage.warning(message);
}

function handleCountdown(payload: Record<string, unknown>) {
  if (payload.examId === examId.value) {
    examStore.remainSeconds = Number(payload.remainTime ?? examStore.remainSeconds);
  }
}

async function bootstrap() {
  await examStore.loadSession(examId.value);
  examSocket = createExamSocket() as unknown as SocketLike;
  examSocket.on('connect', handleSocketConnected);
  examSocket.on('connected', handleSocketConnected);
  examSocket.on('disconnect', handleSocketDisconnect);
  examSocket.on('reconnect', handleSocketReconnect);
  examSocket.on('connect_error', handleSocketConnectError);
  examSocket.on('countdown', handleCountdown);
  syncExamChannel();
  autoSaveTimer = window.setInterval(() => {
    saveProgress(false).catch(() => undefined);
  }, 30000);
  document.addEventListener('visibilitychange', handleVisibilityChange);
}

watch(
  () => examStore.remainSeconds,
  async (value) => {
    if (value <= 0 && !autoSubmitted) {
      autoSubmitted = true;
      await examStore.submit();
      ElMessage.warning('考试时间已到，系统已自动交卷');
      router.replace('/student/exam');
    }
  }
);

onMounted(bootstrap);

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    window.clearInterval(autoSaveTimer);
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  examSocket?.off('connect', handleSocketConnected);
  examSocket?.off('connected', handleSocketConnected);
  examSocket?.off('disconnect', handleSocketDisconnect);
  examSocket?.off('reconnect', handleSocketReconnect);
  examSocket?.off('connect_error', handleSocketConnectError);
  examSocket?.off('countdown', handleCountdown);
  examSocket?.disconnect();
  examStore.setConnected(false);
  examStore.reset();
});
</script>

<style scoped>
.hero-actions{display:flex;flex-wrap:wrap;gap:10px;justify-content:flex-end;align-items:center}@media (max-width:768px){.hero-actions{justify-content:flex-start}}
</style>
