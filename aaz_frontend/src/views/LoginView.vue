<template>
  <v-container fluid class="fill-height page-background">
    <v-row justify="center" align="center">
      <v-col cols="12" sm="8" md="6" lg="4">
        <!-- 系統名稱區塊 -->
        <!-- <v-card class="elevation-8 mb-4 text-center pa-4 system-name">

        </v-card> -->

        <!-- 登入表單 -->
        <v-card class="elevation-8 pa-6" style="background-color: rgba(255, 255, 255, 0.85)">
          <h1 class="logo-title d-flex align-center justify-center mb-6">
            欸
            <img src="/AAZ_icon.png" alt="A" class="logo-icon" />
            誌
          </h1>

          <v-form ref="formRef" v-model="valid" @submit.prevent="handleSubmit">
            <!-- Email 欄位 -->
            <v-text-field
              v-model="email"
              label="Email"
              type="email"
              prepend-inner-icon="mdi-email"
              :rules="emailRules"
              required
              variant="outlined"
              class="mb-2"
            ></v-text-field>

            <!-- 暱稱欄位（僅註冊時顯示） -->
            <v-text-field
              v-if="!isLogin"
              v-model="nickname"
              label="暱稱"
              prepend-inner-icon="mdi-account"
              :rules="nicknameRules"
              required
              variant="outlined"
              class="mb-2"
            ></v-text-field>

            <!-- 密碼欄位 -->
            <v-text-field
              v-model="password"
              :label="isLogin ? '密碼' : '設定密碼'"
              :type="showPassword ? 'text' : 'password'"
              prepend-inner-icon="mdi-lock"
              :append-inner-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
              @click:append-inner="showPassword = !showPassword"
              :rules="passwordRules"
              required
              variant="outlined"
              class="mb-2"
            ></v-text-field>

            <!-- 確認密碼欄位（僅註冊時顯示） -->
            <v-text-field
              v-if="!isLogin"
              v-model="confirmPassword"
              label="確認密碼"
              :type="showConfirmPassword ? 'text' : 'password'"
              prepend-inner-icon="mdi-lock-check"
              :append-inner-icon="showConfirmPassword ? 'mdi-eye' : 'mdi-eye-off'"
              @click:append-inner="showConfirmPassword = !showConfirmPassword"
              :rules="confirmPasswordRules"
              required
              variant="outlined"
              class="mb-4"
            ></v-text-field>

            <!-- 錯誤訊息 -->
            <v-alert
              v-if="errorMessage"
              type="error"
              variant="tonal"
              class="mb-4"
              closable
              @click:close="errorMessage = ''"
            >
              {{ errorMessage }}
            </v-alert>

            <!-- 成功訊息 -->
            <v-alert
              v-if="successMessage"
              type="success"
              variant="tonal"
              class="mb-4"
              closable
              @click:close="successMessage = ''"
            >
              {{ successMessage }}
            </v-alert>

            <!-- 提交按鈕 -->
            <v-btn
              type="submit"
              color="primary"
              size="large"
              block
              :loading="loading"
              :disabled="!valid"
            >
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
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();

// 狀態管理
const isLogin = ref(true);
const valid = ref(false);
const loading = ref(false);
const formRef = ref(null);

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
      successMessage.value = '登入成功！';
      setTimeout(() => {
        router.push('/home');
      }, 500);
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
}

.logo-icon {
  height: 1.15em;
  margin: 0 0.15em;
  vertical-align: middle;
}

.toggle-link {
  color: #4caf50;
  text-decoration: none;
  cursor: pointer;
  font-weight: 500;
}

.toggle-link:hover {
  text-decoration: underline;
}
</style>
