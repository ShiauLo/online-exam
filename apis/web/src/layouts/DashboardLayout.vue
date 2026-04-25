<template>
  <div class="dashboard-layout">
    <aside class="dashboard-layout__sidebar glass-card">
      <div class="dashboard-layout__brand">
        <span class="soft-tag">在线考试系统</span>
        <h1>前端控制台</h1>
        <p>权限、业务、审计与运维统一入口</p>
      </div>

      <el-menu
        :default-active="route.path"
        class="dashboard-layout__menu"
        @select="handleSelect"
      >
        <el-menu-item
          v-for="menu in permissionStore.menus"
          :key="menu.path"
          :index="menu.path"
        >
          <el-icon>
            <component :is="menu.icon" />
          </el-icon>
          <span>{{ menu.menuName }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="dashboard-layout__main">
      <header class="dashboard-layout__header glass-card">
        <div>
          <p class="dashboard-layout__hello">欢迎回来，{{ authStore.user?.realName }}</p>
          <h2>{{ route.meta.title ?? '工作台' }}</h2>
        </div>

        <div class="dashboard-layout__actions">
          <el-badge :value="notifyStore.unreadCount" :max="99">
            <el-button text @click="notifyStore.markAllRead">消息中心</el-button>
          </el-badge>
          <el-button text @click="router.push('/personal')">个人中心</el-button>
          <el-button type="primary" plain @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <main class="page-shell">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useNotifyStore } from '@/stores/notify';
import { usePermissionStore } from '@/stores/permission';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notifyStore = useNotifyStore();
const permissionStore = usePermissionStore();

function handleSelect(path: string) {
  router.push(path);
}

async function handleLogout() {
  await authStore.logout();
  permissionStore.reset();
  router.replace('/login');
}
</script>

<style scoped>
.dashboard-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  min-height: 100vh;
  gap: 18px;
  padding: 18px;
}

.dashboard-layout__sidebar {
  padding: 20px;
}

.dashboard-layout__brand h1 {
  margin: 16px 0 8px;
  font-size: 28px;
}

.dashboard-layout__brand p {
  margin: 0 0 20px;
  color: var(--text-secondary);
  line-height: 1.7;
}

.dashboard-layout__menu {
  border: 0;
  background: transparent;
}

.dashboard-layout__main {
  min-width: 0;
}

.dashboard-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 22px;
}

.dashboard-layout__hello {
  margin: 0 0 6px;
  color: var(--text-secondary);
}

.dashboard-layout__header h2 {
  margin: 0;
  font-size: 24px;
}

.dashboard-layout__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 1024px) {
  .dashboard-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .dashboard-layout__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard-layout__actions {
    flex-wrap: wrap;
  }
}
</style>
