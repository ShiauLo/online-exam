<template>
  <div>
    <section class="page-hero">
      <div>
        <span class="soft-tag">审计员专属页</span>
        <h1 class="page-hero__title">问题跟踪</h1>
        <p class="page-hero__desc">审计员只跟踪问题流转，不执行接单、转派或关闭操作。</p>
      </div>
      <PageToolbar>
        <template #filters>
          <span class="soft-tag" :class="realtimeStatusClass">{{ realtimeStatusText }}</span>
          <span v-if="latestMessage" class="soft-tag soft-tag--warning">{{ latestMessage }}</span>
        </template>
        <template #actions>
          <el-button @click="loadIssues">刷新数据</el-button>
        </template>
      </PageToolbar>
    </section>
    <section class="page-grid page-grid--two">
      <article class="surface-section glass-card">
        <SectionHeader title="问题列表" description="用于追踪问题闭环和处理节奏。" />
        <StatePanel v-if="loadError" type="error" title="问题列表加载失败" :description="loadError" action-text="重新加载" @action="loadIssues" />
        <el-table v-else :data="issues" v-loading="loading" stripe empty-text="暂无问题">
          <el-table-column label="标题" min-width="180"><template #default="{ row }">{{ row.title }}</template></el-table-column>
          <el-table-column label="类型" width="100"><template #default="{ row }"><el-tag round effect="light">{{ row.type }}</el-tag></template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'CLOSED' ? 'success' : 'warning'" round effect="light">{{ row.status }}</el-tag></template></el-table-column>
          <el-table-column label="处理人" width="120"><template #default="{ row }">{{ row.currentHandlerName ?? row.handlerName ?? '-' }}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{ row }"><el-button text type="primary" @click="previewIssue(row)">查看进度</el-button></template></el-table-column>
        </el-table>
      </article>
      <article class="surface-section glass-card">
        <SectionHeader title="处理时间线" description="展示问题从创建到关闭的关键过程。" />
        <TimelineList v-if="timelineItems.length" :items="timelineItems" />
        <StatePanel v-else type="empty" title="暂无时间线" description="请选择一条问题查看进度。" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import PageToolbar from '@/components/PageToolbar.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import StatePanel from '@/components/StatePanel.vue';
import TimelineList from '@/components/TimelineList.vue';
import { useLoadFeedback } from '@/composables/useLoadFeedback';
import { useIssueRealtime } from '@/composables/useIssueRealtime';
import { queryIssues } from '@/api/issue';
type IssueRow = Record<string, any>;
const issues = ref<IssueRow[]>([]);
const { loading, loadError, runLoad } = useLoadFeedback();
const { timeline, latestMessage, realtimeStatusText, realtimeStatusClass, previewIssue, connectIssueRealtime } = useIssueRealtime(loadIssues);
const timelineItems = computed(() =>
  timeline.value.map((item, index) => ({
    key: `${item.time}-${item.title}-${index}`,
    tag: item.time,
    title: item.title,
    description: item.desc
  }))
);
async function loadIssues() {
  await runLoad(async () => {
    const result = await queryIssues({ pageNum: 1, pageSize: 20 });
    issues.value = result.list;
  }, '审计问题列表加载失败，请稍后重试');
}
onMounted(async () => {
  await loadIssues();
  connectIssueRealtime();
});
</script>
