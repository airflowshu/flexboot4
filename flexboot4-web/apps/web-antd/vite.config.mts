import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api/admin': {
            target: 'http://192.168.6.7:8080/api/admin',
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/api\/admin/, ''),
            ws: true,
          },
          '/api/ai': {
            changeOrigin: true,
            target: 'http://192.168.6.7:8081/api/ai',
            rewrite: (path: string) => path.replace(/^\/api\/ai/, ''),
          },
        },
      },
    },
  };
}) as any;
