import { defineStore } from 'pinia';
import { fetchExamRealtimeSession, saveExamProgress, submitExam } from '@/api/examRealtime';

export const useExamStore = defineStore('exam', {
  state: () => ({
    examId: '',
    examName: '',
    remainSeconds: 0,
    questions: [] as Array<Record<string, unknown>>,
    answers: {} as Record<string, string>,
    connected: false,
    screenOutCount: 0
  }),
  getters: {
    remainLabel: (state) => {
      const minute = Math.floor(state.remainSeconds / 60);
      const second = state.remainSeconds % 60;
      return `${`${minute}`.padStart(2, '0')}:${`${second}`.padStart(2, '0')}`;
    }
  },
  actions: {
    async loadSession(examId: string) {
      const result = await fetchExamRealtimeSession({ examId });
      const exam = (result.exam as Record<string, unknown>) ?? {};
      this.examId = examId;
      this.examName = String(exam.examName ?? '考试答题');
      this.questions = (result.questions as Array<Record<string, unknown>>) ?? [];
      this.answers = ((result.answers as Record<string, string>) ?? {});
      this.screenOutCount = Number(result.screenOutCount ?? 0);
      this.remainSeconds = Number(exam.remainSeconds ?? Number(exam.duration ?? 90) * 60);
    },
    setConnected(value: boolean) {
      this.connected = value;
    },
    tick() {
      if (this.remainSeconds > 0) {
        this.remainSeconds -= 1;
      }
    },
    setAnswer(questionId: string, value: string) {
      this.answers = {
        ...this.answers,
        [questionId]: value
      };
    },
    async persistProgress() {
      if (!this.examId) {
        return;
      }
      await saveExamProgress({
        examId: this.examId,
        answers: this.answers
      });
    },
    async submit() {
      if (!this.examId) {
        return;
      }
      await submitExam({
        examId: this.examId,
        answers: this.answers
      });
    },
    increaseScreenOut() {
      this.screenOutCount += 1;
    },
    reset() {
      this.examId = '';
      this.examName = '';
      this.remainSeconds = 0;
      this.questions = [];
      this.answers = {};
      this.connected = false;
      this.screenOutCount = 0;
    }
  }
});
