import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api from '@/api/axios';

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || null);
  const id = ref(localStorage.getItem('userId') || null);
  const email = ref(localStorage.getItem('userEmail') || null);
  const nickname = ref(localStorage.getItem('userNickname') || null);

  const isLoggedIn = computed(() => !!token.value);

  async function login(loginData) {
    const response = await api.post('/auth/login', loginData);
    token.value = response.data.token;
    id.value = response.data.id;
    email.value = response.data.email;
    nickname.value = response.data.nickname;

    localStorage.setItem('token', response.data.token);
    localStorage.setItem('userId', response.data.id);
    localStorage.setItem('userEmail', response.data.email);
    localStorage.setItem('userNickname', response.data.nickname);

    return response.data;
  }

  async function register(signupData) {
    const response = await api.post('/auth/register', signupData);
    return response.data;
  }

  function logout() {
    token.value = null;
    id.value = null;
    email.value = null;
    nickname.value = null;

    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userNickname');
  }

  function loadUserFromStorage() {
    token.value = localStorage.getItem('token');
    id.value = localStorage.getItem('userId');
    email.value = localStorage.getItem('userEmail');
    nickname.value = localStorage.getItem('userNickname');
  }

  return {
    token,
    id,
    email,
    nickname,
    isLoggedIn,
    login,
    register,
    logout,
    loadUserFromStorage,
  };
});
