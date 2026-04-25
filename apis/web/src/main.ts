import { createApp, type Component } from 'vue';
import {
  Avatar,
  Bell,
  BellFilled,
  ChatDotSquare,
  Checked,
  Clock,
  Collection,
  CollectionTag,
  Connection,
  DataAnalysis,
  Document,
  DocumentChecked,
  Files,
  FolderChecked,
  FolderOpened,
  Histogram,
  HomeFilled,
  Lock,
  Memo,
  MessageBox,
  Notebook,
  Reading,
  School,
  Setting,
  Tickets,
  Tools,
  TrendCharts,
  User,
  WarningFilled
} from '@element-plus/icons-vue';
import 'element-plus/es/components/message/style/css';
import 'element-plus/es/components/message-box/style/css';
import App from './App.vue';
import { router } from './router';
import { pinia } from './stores';
import './assets/main.css';

const app = createApp(App);

const usedIcons: Record<string, Component> = {
  Avatar,
  Bell,
  BellFilled,
  ChatDotSquare,
  Checked,
  Clock,
  Collection,
  CollectionTag,
  Connection,
  DataAnalysis,
  Document,
  DocumentChecked,
  Files,
  FolderChecked,
  FolderOpened,
  Histogram,
  HomeFilled,
  Lock,
  Memo,
  MessageBox,
  Notebook,
  Reading,
  School,
  Setting,
  Tickets,
  Tools,
  TrendCharts,
  User,
  WarningFilled
};

Object.entries(usedIcons).forEach(([key, component]) => {
  app.component(key, component);
});

app.use(pinia);
app.use(router);
app.mount('#app');
