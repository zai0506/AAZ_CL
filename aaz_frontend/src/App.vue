<template>
  <v-app>
    <BackgroundSlideshow />
    <router-view v-if="appReady" />
    <div v-else class="loading-container">
      <v-progress-circular indeterminate color="primary"></v-progress-circular>
    </div>

    <!-- 全局個人檔案對話框 -->
    <ProfileDialog v-model="profileDialogStore.showDialog" />
  </v-app>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { useProfileDialogStore } from '@/stores/profileDialog';
import axios from '@/api/axios';
import ProfileDialog from '@/components/ProfileDialog.vue';
import BackgroundSlideshow from '@/components/BackgroundSlideshow.vue';

const userStore = useUserStore();
const profileDialogStore = useProfileDialogStore();
const appReady = ref(false);

onMounted(async () => {
  // 如果 localStorage 有 token，先驗證是否有效
  const token = localStorage.getItem('token');
  if (token) {
    try {
      // 發送一個簡單的請求來驗證 token
      await axios.get('/trips');
    } catch (error) {
      // 如果是 401，axios 攔截器會自動清除 localStorage 並跳轉到登入頁
      // 其他錯誤（如網絡錯誤）則忽略，讓用戶正常進入應用
    }
  }
  appReady.value = true;
});
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Roboto', sans-serif;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
</style>
