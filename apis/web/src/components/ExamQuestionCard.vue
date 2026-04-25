<template>
  <article class="surface-section glass-card question-card">
    <div class="question-card__head">
      <div>
        <span class="soft-tag">第 {{ index + 1 }} 题</span>
        <h3>{{ question.stem }}</h3>
      </div>
      <el-tag :type="question.type === 'subjective' ? 'warning' : 'success'" effect="light" round>
        {{ questionTypeLabel(question.type) }}
      </el-tag>
    </div>

    <el-radio-group
      v-if="question.type === 'single'"
      :model-value="singleAnswer"
      class="option-list"
      @change="emitSingleChange"
    >
      <el-radio v-for="option in normalizedOptions" :key="option" :value="option">{{ option }}</el-radio>
    </el-radio-group>

    <el-checkbox-group
      v-else-if="question.type === 'multi'"
      :model-value="multiAnswer"
      class="option-list"
      @change="emitMultiChange"
    >
      <el-checkbox v-for="option in normalizedOptions" :key="option" :value="option">{{ option }}</el-checkbox>
    </el-checkbox-group>

    <el-input
      v-else
      :model-value="singleAnswer"
      type="textarea"
      :rows="5"
      placeholder="请输入主观题答案"
      @input="emitSubjectiveChange"
    />
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue';

type QuestionRow = Record<string, any>;

const props = withDefaults(
  defineProps<{
    index: number;
    question: QuestionRow;
    singleAnswer?: string;
    multiAnswer?: string[];
  }>(),
  {
    singleAnswer: '',
    multiAnswer: () => []
  }
);

const emit = defineEmits<{
  'update-answer': [
    payload: {
      questionId: string;
      type: 'single' | 'multi' | 'subjective';
      value: string | string[];
    }
  ];
}>();

const normalizedOptions = computed(() =>
  Array.isArray(props.question.options) ? props.question.options.map((item) => String(item)) : []
);

function questionTypeLabel(type: unknown) {
  if (type === 'single') return '单选题';
  if (type === 'multi') return '多选题';
  return '主观题';
}

function emitSingleChange(value: string | number | boolean) {
  emit('update-answer', {
    questionId: String(props.question.questionId ?? ''),
    type: 'single',
    value: String(value)
  });
}

function emitMultiChange(value: unknown) {
  emit('update-answer', {
    questionId: String(props.question.questionId ?? ''),
    type: 'multi',
    value: Array.isArray(value) ? value.map((item) => String(item)) : []
  });
}

function emitSubjectiveChange(value: string | number) {
  emit('update-answer', {
    questionId: String(props.question.questionId ?? ''),
    type: 'subjective',
    value: String(value)
  });
}
</script>

<style scoped>
.option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.question-card {
  display: grid;
  gap: 18px;
}

.question-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.question-card__head h3 {
  margin: 12px 0 0;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .question-card__head {
    flex-direction: column;
  }
}
</style>
