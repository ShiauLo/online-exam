import path from 'node:path';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default defineConfig({
  plugins: [
    vue(),
    Components({
      dirs: [],
      dts: false,
      resolvers: [ElementPlusResolver({ importStyle: 'css' })]
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return;
          }

          if (id.includes('/dayjs/')) {
            return 'vendor-dayjs';
          }

          if (id.includes('/async-validator/')) {
            return 'vendor-async-validator';
          }

          if (id.includes('/@floating-ui/')) {
            return 'vendor-floating-ui';
          }

          if (
            id.includes('/vue/') ||
            id.includes('/vue-router/') ||
            id.includes('/pinia/')
          ) {
            return 'vendor-vue';
          }

          if (id.includes('/axios/')) {
            return 'vendor-axios';
          }

          if (id.includes('/socket.io-client/')) {
            return 'vendor-socket';
          }
        }
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/tests/setup.ts',
    css: true,
    server: {
      deps: {
        inline: ['element-plus']
      }
    }
  }
});
