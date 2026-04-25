import ElementPlus from 'element-plus';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import InfoPreviewCard from '@/components/InfoPreviewCard.vue';
import MetricsOverview from '@/components/MetricsOverview.vue';
import PageToolbar from '@/components/PageToolbar.vue';
import PaginationFooter from '@/components/PaginationFooter.vue';
import SectionHeader from '@/components/SectionHeader.vue';
import TimelineList from '@/components/TimelineList.vue';

describe('共享展示组件', () => {
  it('分区标题组件会展示标题、描述和扩展区', () => {
    const wrapper = mount(SectionHeader, {
      props: {
        title: '成绩列表',
        description: '用于查看成绩概览'
      },
      slots: {
        extra: '<span>共 12 条</span>'
      }
    });

    expect(wrapper.text()).toContain('成绩列表');
    expect(wrapper.text()).toContain('用于查看成绩概览');
    expect(wrapper.text()).toContain('共 12 条');
  });

  it('详情预览卡组件会展示标记、标题和明细行', () => {
    const wrapper = mount(InfoPreviewCard, {
      props: {
        badge: '已发布',
        title: 'Java 阶段测验',
        lines: ['试卷编号：p-1', '开始时间：2026-04-19 09:00:00']
      }
    });

    expect(wrapper.text()).toContain('已发布');
    expect(wrapper.text()).toContain('Java 阶段测验');
    expect(wrapper.text()).toContain('试卷编号：p-1');
    expect(wrapper.text()).toContain('开始时间：2026-04-19 09:00:00');
  });

  it('时间线组件会展示多个节点', () => {
    const wrapper = mount(TimelineList, {
      props: {
        items: [
          { key: '1', tag: '09:00', title: '已接单', description: '教师已接单处理' },
          { key: '2', tag: '09:05', title: '已关闭', description: '问题已解决并关闭' }
        ]
      }
    });

    expect(wrapper.text()).toContain('09:00');
    expect(wrapper.text()).toContain('已接单');
    expect(wrapper.text()).toContain('教师已接单处理');
    expect(wrapper.text()).toContain('09:05');
    expect(wrapper.text()).toContain('已关闭');
  });

  it('指标卡组件会展示多个指标项', () => {
    const wrapper = mount(MetricsOverview, {
      props: {
        items: [
          { label: '考试总数', value: 12 },
          { label: '进行中', value: 3 }
        ]
      }
    });

    expect(wrapper.text()).toContain('考试总数');
    expect(wrapper.text()).toContain('12');
    expect(wrapper.text()).toContain('进行中');
    expect(wrapper.text()).toContain('3');
  });

  it('分页组件会透传分页变更事件', async () => {
    const wrapper = mount(PaginationFooter, {
      props: {
        currentPage: 1,
        pageSize: 10,
        total: 35
      },
      global: {
        plugins: [ElementPlus]
      }
    });

    const pagination = wrapper.findComponent({ name: 'ElPagination' });
    pagination.vm.$emit('current-change', 3);

    expect(wrapper.emitted('change')).toEqual([[3]]);
  });

  it('页面工具栏组件会同时渲染筛选区和操作区', () => {
    const wrapper = mount(PageToolbar, {
      slots: {
        filters: '<input value="关键词检索" />',
        actions: '<button>查询</button><button>新增</button>'
      }
    });

    expect(wrapper.text()).toContain('查询');
    expect(wrapper.text()).toContain('新增');
    expect(wrapper.html()).toContain('关键词检索');
  });
});
