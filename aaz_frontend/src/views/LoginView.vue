<template>
  <div class="login-page-wrapper">
    <!-- 明信片拼圖背景 -->
    <div class="postcard-background">
      <div v-for="(tile, index) in backgroundTiles" :key="tile.id" class="postcard-tile" :style="{
        backgroundImage: `url(/background/${tile.imageId}.jpg)`,
        transform: `rotate(${tile.rotation}deg)`,
        top: tile.position.top,
        left: tile.position.left,
        zIndex: tile.zIndex
      }">
        <!-- 新圖片淡入層 -->
        <div class="postcard-overlay" :style="{
          backgroundImage: tile.nextImageId ? `url(/background/${tile.nextImageId}.jpg)` : 'none',
          opacity: tile.nextImageId ? 1 : 0
        }"></div>
      </div>
    </div>

    <v-container fluid class="fill-height login-content">
      <v-row justify="center" align="center">
        <v-col cols="12" sm="8" md="6" lg="4">
          <!-- 系統名稱區塊 -->
          <!-- <v-card class="elevation-8 mb-4 text-center pa-4 system-name">

        </v-card> -->

          <!-- 登入表單 -->
          <v-card class="elevation-8 pa-6" :class="{ 'animating-container': showSuccessAnimation }"
            style="background-color: rgba(255, 255, 255, 0.85); transition: all 0.5s ease;">
            <h1 class="logo-title d-flex align-center justify-center mb-6">
              欸
              <img src="/AAZ_icon.png" alt="A" class="logo-icon"
                :class="{ 'animate-logo': showSuccessAnimation, 'logo-enlarged': showSuccessAnimation }" />
              誌
            </h1>

            <template v-if="!showSuccessAnimation">
              <v-form ref="formRef" v-model="valid" @submit.prevent="handleSubmit">
                <!-- Email 欄位 -->
                <v-text-field v-model="email" label="Email" type="email" prepend-inner-icon="mdi-email"
                  :rules="emailRules" required variant="outlined" class="mb-2"></v-text-field>

                <!-- 暱稱欄位（僅註冊時顯示） -->
                <v-text-field v-if="!isLogin" v-model="nickname" label="暱稱" prepend-inner-icon="mdi-account"
                  :rules="nicknameRules" required variant="outlined" class="mb-2"></v-text-field>

                <!-- 密碼欄位 -->
                <v-text-field v-model="password" :label="isLogin ? '密碼' : '設定密碼'"
                  :type="showPassword ? 'text' : 'password'" prepend-inner-icon="mdi-lock"
                  :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                  @click:append-inner="showPassword = !showPassword" :rules="passwordRules" required variant="outlined"
                  class="mb-2"></v-text-field>

                <!-- 確認密碼欄位（僅註冊時顯示） -->
                <v-text-field v-if="!isLogin" v-model="confirmPassword" label="確認密碼"
                  :type="showConfirmPassword ? 'text' : 'password'" prepend-inner-icon="mdi-lock-check"
                  :append-inner-icon="showConfirmPassword ? 'mdi-eye' : 'mdi-eye-off'"
                  @click:append-inner="showConfirmPassword = !showConfirmPassword" :rules="confirmPasswordRules"
                  required variant="outlined" class="mb-4"></v-text-field>

                <!-- 錯誤訊息 -->
                <v-alert v-if="errorMessage" type="error" variant="tonal" class="mb-4" closable
                  @click:close="errorMessage = ''">
                  {{ errorMessage }}
                </v-alert>

                <!-- 成功訊息 -->
                <v-alert v-if="successMessage" type="success" variant="tonal" class="mb-4" closable
                  @click:close="successMessage = ''">
                  {{ successMessage }}
                </v-alert>

                <!-- 提交按鈕 -->
                <v-btn type="submit" color="#5470C6" size="large" block :loading="loading" :disabled="!valid">
                  {{ isLogin ? '登入' : '註冊' }}
                </v-btn>
              </v-form>

              <!-- 切換登入/註冊連結 -->
              <div class="text-center mt-4">
                <template v-if="isLogin">
                  <span class="text-body-2">還沒有帳號？</span>
                  <a @click="toggleMode" class="toggle-link text-body-2">註冊</a>
                  <span class="text-body-2">一個吧</span>
                </template>
                <template v-else>
                  <span class="text-body-2">已經有帳號了？</span>
                  <a @click="toggleMode" class="toggle-link text-body-2">登入</a>
                </template>
              </div>
            </template>
          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();

// 明信片拼圖背景設定
// 如果之後在 public/background 加照片，只要更新這個數字即可
// 照片檔名需要是連續數字：1.jpg, 2.jpg, 3.jpg ... N.jpg
const totalImages = 27; // public/background 裡有 1.jpg 到 27.jpg
const backgroundTiles = ref([]);
let refreshInterval = null;

// 隨機生成圖片ID（1-N）
const getRandomImageId = () => Math.floor(Math.random() * totalImages) + 1;

// 初始化背景拼圖
const initBackgroundTiles = () => {
  // 7 張照片，固定位置（更均勻分佈，減少上方留白）
  const fixedPositions = [
    { top: '-5%', left: '3%', rotation: -12, zIndex: 10 },
    { top: '2%', left: '62%', rotation: 18, zIndex: 15 },
    { top: '20%', left: '18%', rotation: -8, zIndex: 20 },
    { top: '15%', left: '78%', rotation: 12, zIndex: 5 },
    { top: '42%', left: '8%', rotation: 15, zIndex: 12 },
    { top: '38%', left: '55%', rotation: -18, zIndex: 8 },
    { top: '48%', left: '82%', rotation: 8, zIndex: 18 }
  ];

  backgroundTiles.value = fixedPositions.map((pos, index) => ({
    id: index,
    imageId: getRandomImageId(),
    nextImageId: null, // 用於淡入的新圖片
    rotation: pos.rotation,
    position: { top: pos.top, left: pos.left },
    zIndex: pos.zIndex
  }));
};

// 隨機更換幾個格子的圖片（新照片淡入覆蓋舊照片）
const refreshRandomTiles = () => {
  const numTilesToRefresh = Math.floor(Math.random() * 2) + 1; // 每次更換1-2個格子
  const tilesToRefresh = [];

  while (tilesToRefresh.length < numTilesToRefresh) {
    const randomIndex = Math.floor(Math.random() * backgroundTiles.value.length);
    if (!tilesToRefresh.includes(randomIndex)) {
      tilesToRefresh.push(randomIndex);
    }
  }

  tilesToRefresh.forEach(index => {
    const tile = backgroundTiles.value[index];
    // 設置新圖片到 overlay 層，會自動淡入
    tile.nextImageId = getRandomImageId();

    // 800ms 後（淡入完成），將新圖設為主圖，清除 overlay
    setTimeout(() => {
      tile.imageId = tile.nextImageId;
      tile.nextImageId = null;
    }, 800);
  });
};

onMounted(() => {
  initBackgroundTiles();
  console.log('Background tiles initialized:', backgroundTiles.value);
  // 每4秒隨機更換一些格子
  refreshInterval = setInterval(refreshRandomTiles, 3000);
});

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});

// 狀態管理
const isLogin = ref(true);
const valid = ref(false);
const loading = ref(false);
const formRef = ref(null);
const showSuccessAnimation = ref(false); // 用於控制成功動畫

// 表單欄位
const email = ref('');
const nickname = ref('');
const password = ref('');
const confirmPassword = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);

// 訊息
const errorMessage = ref('');
const successMessage = ref('');

// 驗證規則
const emailRules = [
  (v) => !!v || 'Email 為必填欄位',
  (v) => /.+@.+\..+/.test(v) || 'Email 格式不正確',
];

const nicknameRules = [
  (v) => !!v || '暱稱為必填欄位',
  (v) => (v && v.length >= 2) || '暱稱至少需要 2 個字',
  (v) => (v && v.length <= 50) || '暱稱不能超過 50 個字',
];

const passwordRules = [
  (v) => !!v || '密碼為必填欄位',
  (v) => (v && v.length >= 6) || '密碼至少需要 6 個字',
];

const confirmPasswordRules = computed(() => [
  (v) => !!v || '請確認密碼',
  (v) => v === password.value || '兩次密碼輸入不一致',
]);

// 切換登入/註冊模式
const toggleMode = () => {
  isLogin.value = !isLogin.value;
  errorMessage.value = '';
  successMessage.value = '';
  // 清空表單
  email.value = '';
  nickname.value = '';
  password.value = '';
  confirmPassword.value = '';
  if (formRef.value) {
    formRef.value.reset();
  }
};

// 處理提交
const handleSubmit = async () => {
  if (!valid.value) return;

  loading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    if (isLogin.value) {
      // 登入
      await userStore.login({
        email: email.value,
        password: password.value,
      });
      // 登入成功，觸發動畫
      showSuccessAnimation.value = true;
      password.value = ''; // 清空密碼欄

      // 等待動畫播放完畢後跳轉
      setTimeout(() => {
        router.push('/home');
      }, 1400); // 總動畫時長 1.5s，在結束前 0.1s 開始跳轉
    } else {
      // 註冊
      await userStore.register({
        email: email.value,
        nickname: nickname.value,
        password: password.value,
      });
      successMessage.value = '註冊成功！請登入';
      setTimeout(() => {
        isLogin.value = true;
        password.value = '';
        confirmPassword.value = '';
      }, 1500);
    }
  } catch (error) {
    errorMessage.value =
      error.response?.data?.message ||
      (isLogin.value ? '登入失敗，請檢查帳號密碼' : '註冊失敗，Email 可能已被使用');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* 登入頁面包裝器 */
.login-page-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* 明信片拼圖背景 */
.postcard-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  background-image: url('/woodpic.jpg');
  background-size: cover;
  pointer-events: none;
  overflow: hidden;
  opacity: 0.7;
}

.postcard-tile {
  position: absolute;
  width: 600px;
  height: 450px;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-color: #f8f8f8;
  box-shadow:
    0 12px 32px rgba(0, 0, 0, 0.4),
    0 4px 12px rgba(0, 0, 0, 0.3);
  border: 16px solid white;
  border-radius: 2px;
  overflow: hidden;
}

/* 新圖片淡入層 */
.postcard-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: contain;
  background-position: center;
  background-repeat: no-repeat;
  background-color: #f8f8f8;
  opacity: 0;
  transition: opacity 0.8s ease-in-out;
  pointer-events: none;
}

.postcard-tile::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg,
      rgba(255, 255, 255, 0.1) 0%,
      transparent 50%,
      rgba(0, 0, 0, 0.05) 100%);
  pointer-events: none;
}

/* 登入內容容器 */
.login-content {
  position: relative;
  z-index: 200;
  background: transparent !important;
}

.login-container {
  background-color: rgba(255, 255, 255, 0.85);
  min-height: 100vh;
}

.system-name {
  background: white;
}

.primary--text {

  color: #5188d0;
}

.logo-title {
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
  font-size: 3.5rem;
  /* 放大字體 */
  transition: font-size 0.4s ease;
}

.logo-icon {
  height: 1.15em;
  margin: 0 0.8em;
  /* 再次拉開間距 */
  vertical-align: middle;
  transition: transform 0.4s ease;
}

.toggle-link {
  color: #5470C6;
  text-decoration: none;
  cursor: pointer;
  font-weight: 500;
}

.toggle-link:hover {
  text-decoration: underline;
}

/* --- Login Success Animation --- */

/* 動畫時，v-card 的樣式 */
.animating-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 440px;
  /* 縮小白框高度 */
}

.animating-container .logo-title {
  font-size: 4rem;
  /* 動畫時字體再放大 */
  margin-bottom: 0 !important;
  /* 動畫時移除 logo title 的 bottom margin */
}

/* Logo 動畫: 跳躍(0.6s) -> 暫停(0.1s) -> 跳躍(0.6s) = 總長 1.3s */
@keyframes jump-and-rotate {
  0% {
    transform: translateY(0) scale(2.5) rotate(0deg);
  }

  23% {
    /* 第一跳的中點，旋轉 360 度 */
    transform: translateY(-75px) scale(2.5) rotate(360deg);
  }

  46% {
    /* 第一跳結束，旋轉 720 度 */
    transform: translateY(0) scale(2.5) rotate(720deg);
  }

  54% {
    /* 暫停後，第二跳開始，起始旋轉角度為 720 度 */
    transform: translateY(0) scale(2.5) rotate(720deg);
  }

  77% {
    /* 第二跳的中點，旋轉 720 + 360 = 1080 度 */
    transform: translateY(-75px) scale(2.5) rotate(1080deg);
  }

  100% {
    /* 第二跳結束，旋轉 720 + 720 = 1440 度 */
    transform: translateY(0) scale(2.5) rotate(1440deg);
  }
}

.animate-logo {
  animation: jump-and-rotate 1.5s ease-in-out;
  animation-fill-mode: forwards;
  /* 保持動畫結束時的狀態 */
}
</style>
