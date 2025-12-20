import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 請求攔截器（自動加入 Token）
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 響應攔截器（處理 401 自動登出）
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 如果請求配置中有 skipAutoLogout 標記，則不自動登出
      if (error.config?.skipAutoLogout) {
        return Promise.reject(error);
      }

      // 清除所有用戶相關的 localStorage
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('userEmail');
      localStorage.removeItem('userNickname');

      // 更新 Pinia store 狀態
      const { useUserStore } = await import('@/stores/user');
      const userStore = useUserStore();
      userStore.logout();

      // 動態導入 router 以避免循環依賴
      const { default: router } = await import('@/router');
      router.push('/login').catch(() => {});
    }
    return Promise.reject(error);
  }
);

export default api;
