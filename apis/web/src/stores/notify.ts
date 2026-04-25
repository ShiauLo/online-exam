import { defineStore } from 'pinia';

export interface NotifyItem {
  id: string;
  title: string;
  desc: string;
  time: string;
  read: boolean;
}

export const useNotifyStore = defineStore('notify', {
  state: () => ({
    list: [
      { id: 'n-1', title: '考试告警', desc: 'Java 阶段测验出现切屏告警。', time: '10:24', read: false },
      { id: 'n-2', title: '问题更新', desc: '学生异常申报已进入处理中。', time: '09:55', read: false }
    ] as NotifyItem[]
  }),
  getters: {
    unreadCount: (state) => state.list.filter((item) => !item.read).length
  },
  actions: {
    markAllRead() {
      this.list = this.list.map((item) => ({ ...item, read: true }));
    },
    push(item: NotifyItem) {
      this.list.unshift(item);
    }
  }
});
