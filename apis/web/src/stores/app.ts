import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    lastRequestId: '',
    pageLoading: false
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    },
    setLastRequestId(requestId: string) {
      this.lastRequestId = requestId;
    },
    setPageLoading(value: boolean) {
      this.pageLoading = value;
    }
  }
});
