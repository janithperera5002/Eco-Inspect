import { defineConfig } from 'vite'

export default defineConfig({
  root: '.',
  server: {
    port: 3000,
    open: '/pages/auth/login.html',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: 'dist',
    rollupOptions: {
      input: {
        login:     'pages/auth/login.html',
        dashboard: 'pages/dashboard/home.html',
        reports:   'pages/reports/list.html',
        reportDetail: 'pages/reports/detail.html',
        cases:     'pages/cases/list.html',
        caseDetail: 'pages/cases/detail.html',
        analytics: 'pages/dashboard/analytics.html',
        map:       'pages/map/map.html',
        users:     'pages/users/list.html',
        profile:   'pages/profile/profile.html',
        notifications: 'pages/notifications/list.html'
      }
    }
  }
})
