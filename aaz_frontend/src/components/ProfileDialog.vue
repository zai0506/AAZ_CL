<template>
  <v-dialog v-model="dialogVisible" max-width="500" persistent>
    <v-card>
      <v-card-title style="background-color: rgb(85, 214, 194); color: white;">
        我的檔案
      </v-card-title>
      <v-card-text class="pt-6">
        <!-- 使用者頭像 -->
        <div class="text-center mb-6">
          <v-avatar color="rgb(85, 214, 194)" size="80" class="profile-avatar">
            <v-icon color="white" size="50">{{ profileDialogStore.userIcon }}</v-icon>
          </v-avatar>
        </div>

        <!-- 成功動畫 -->
        <div v-if="showSuccess" class="success-container">
          <div class="success-checkmark">
            <div class="check-icon">
              <span class="icon-line line-tip"></span>
              <span class="icon-line line-long"></span>
              <div class="icon-circle"></div>
              <div class="icon-fix"></div>
            </div>
          </div>
          <div class="success-text">修改成功</div>
        </div>

        <!-- 查看模式 -->
        <v-list v-else-if="!isEditing">
          <v-list-item>
            <v-list-item-title class="text-subtitle-2 text-grey-darken-1 mb-1">暱稱</v-list-item-title>
            <v-list-item-subtitle class="text-body-1 text-black">{{ userStore.nickname }}</v-list-item-subtitle>
          </v-list-item>

          <v-divider class="my-2"></v-divider>

          <v-list-item>
            <v-list-item-title class="text-subtitle-2 text-grey-darken-1 mb-1">E-mail</v-list-item-title>
            <v-list-item-subtitle class="text-body-1 text-black">{{ userStore.email }}</v-list-item-subtitle>
          </v-list-item>

          <v-divider class="my-2"></v-divider>

          <v-list-item>
            <v-list-item-title class="text-subtitle-2 text-grey-darken-1 mb-1">密碼</v-list-item-title>
            <v-list-item-subtitle class="text-body-1 text-black">********</v-list-item-subtitle>
          </v-list-item>
        </v-list>

        <!-- 編輯模式 -->
        <div v-else>
          <v-text-field v-model="form.nickname" label="暱稱" variant="outlined" density="compact"
            class="mb-3"></v-text-field>

          <v-text-field v-model="form.email" label="E-mail" type="email" variant="outlined" density="compact"
            class="mb-3"></v-text-field>

          <v-divider class="my-4"></v-divider>

          <!-- 密碼區域 -->
          <div class="d-flex align-center mb-3">
            <div class="flex-grow-1">
              <div class="text-subtitle-2 text-grey-darken-1 mb-1">密碼</div>
              <div v-if="!showPasswordEdit" class="text-body-1">********</div>
            </div>
            <v-btn color="red" size="small" variant="elevated" @click="showPasswordEdit = !showPasswordEdit">
              {{ showPasswordEdit ? '取消變更' : '變更密碼' }}
            </v-btn>
          </div>

          <!-- 密碼編輯欄位 (條件顯示) -->
          <div v-if="showPasswordEdit">
            <v-text-field v-model="form.oldPassword" label="輸入舊密碼" type="password" variant="outlined" density="compact"
              class="mb-1" :error="!!errors.oldPassword" autocomplete="new-password"></v-text-field>
            <div v-if="errors.oldPassword" class="text-red text-caption mb-2">
              {{ errors.oldPassword }}
            </div>

            <v-text-field v-model="form.newPassword" label="輸入新密碼" type="password" variant="outlined" density="compact"
              class="mb-1" :error="!!errors.newPassword" autocomplete="new-password"></v-text-field>
            <div v-if="errors.newPassword" class="text-red text-caption mb-2">
              {{ errors.newPassword }}
            </div>

            <v-text-field v-model="form.confirmPassword" label="確認新密碼" type="password" variant="outlined"
              density="compact" class="mb-1" :error="!!errors.confirmPassword"
              autocomplete="new-password"></v-text-field>
            <div v-if="errors.confirmPassword" class="text-red text-caption mb-2">
              {{ errors.confirmPassword }}
            </div>
          </div>
        </div>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <template v-if="showSuccess">
          <v-btn variant="text" @click="closeSuccess">關閉</v-btn>
        </template>
        <template v-else-if="!isEditing">
          <v-btn color="rgb(85, 214, 194)" class="text-white" variant="elevated" @click="startEdit">修改個人資料</v-btn>
          <v-btn variant="text" @click="close">關閉</v-btn>
        </template>
        <template v-else>
          <v-btn color="primary" variant="elevated" @click="submitUpdate">送出</v-btn>
          <v-btn variant="text" @click="cancelEdit">取消</v-btn>
        </template>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';

const userStore = useUserStore();

const dialogVisible = defineModel({ type: Boolean, default: false });

const isEditing = ref(false);
const showPasswordEdit = ref(false);
const showSuccess = ref(false);
const form = ref({
  nickname: '',
  email: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});
const errors = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const startEdit = () => {
  isEditing.value = true;
  showPasswordEdit.value = false;
  form.value.nickname = userStore.nickname;
  form.value.email = userStore.email;
  form.value.oldPassword = '';
  form.value.newPassword = '';
  form.value.confirmPassword = '';
  errors.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
};

const cancelEdit = () => {
  isEditing.value = false;
  showPasswordEdit.value = false;
  form.value = {
    nickname: '',
    email: '',
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
  errors.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };
};

const close = () => {
  dialogVisible.value = false;
};

const closeSuccess = () => {
  showSuccess.value = false;
  dialogVisible.value = false;
};

const submitUpdate = async () => {
  // 重置錯誤訊息
  errors.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  // 驗證密碼相關欄位（如果有填寫的話）
  const hasPasswordChange = showPasswordEdit.value && (form.value.oldPassword || form.value.newPassword || form.value.confirmPassword);

  if (hasPasswordChange) {
    // 檢查是否所有密碼欄位都有填寫
    if (!form.value.oldPassword || !form.value.newPassword || !form.value.confirmPassword) {
      if (!form.value.oldPassword) errors.value.oldPassword = '請輸入舊密碼';
      if (!form.value.newPassword) errors.value.newPassword = '請輸入新密碼';
      if (!form.value.confirmPassword) errors.value.confirmPassword = '請確認新密碼';
      return;
    }

    // 檢查新密碼與確認密碼是否一致
    if (form.value.newPassword !== form.value.confirmPassword) {
      errors.value.confirmPassword = '新密碼與確認密碼不一致';
      return;
    }

    // 檢查新密碼是否與舊密碼相同
    if (form.value.oldPassword === form.value.newPassword) {
      errors.value.newPassword = '新密碼不得與舊密碼相同';
      return;
    }
  }

  try {
    // 準備更新資料
    const updateData = {
      nickname: form.value.nickname,
      email: form.value.email
    };

    // 如果有修改密碼，加入密碼欄位
    if (hasPasswordChange) {
      updateData.oldPassword = form.value.oldPassword;
      updateData.newPassword = form.value.newPassword;
    }

    // 發送更新請求
    const response = await axios.put('/auth/profile', updateData);

    // 更新成功，更新 store 中的資料
    userStore.nickname = response.data.nickname;
    userStore.email = response.data.email;
    localStorage.setItem('userNickname', response.data.nickname);
    localStorage.setItem('userEmail', response.data.email);

    // 顯示成功動畫
    isEditing.value = false;
    showPasswordEdit.value = false;
    showSuccess.value = true;
  } catch (error) {
    console.error('修改個人資料失敗:', error);

    // 如果是舊密碼錯誤
    if (error.response?.status === 400 && error.response?.data?.message?.includes('密碼')) {
      errors.value.oldPassword = '舊密碼不符';
    } else {
      alert('更新失敗：' + (error.response?.data?.message || '請稍後再試'));
    }
  }
};

// 監聽對話框關閉，重置編輯狀態
watch(dialogVisible, (newValue) => {
  if (!newValue) {
    isEditing.value = false;
    showPasswordEdit.value = false;
    showSuccess.value = false;
    form.value = {
      nickname: '',
      email: '',
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    errors.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
  }
});
</script>

<style scoped>
/* 個人檔案頭像 */
.profile-avatar {
  box-shadow: 0 4px 12px rgba(85, 214, 194, 0.4);
  border: 3px solid white;
}

.success-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.success-checkmark {
  width: 80px;
  height: 80px;
  margin: 0 auto;
}

.check-icon {
  width: 80px;
  height: 80px;
  position: relative;
  border-radius: 50%;
  box-sizing: content-box;
  border: 4px solid #4caf50;
}

.check-icon::before {
  top: 3px;
  left: -2px;
  width: 30px;
  transform-origin: 100% 50%;
  border-radius: 100px 0 0 100px;
}

.check-icon::after {
  top: 0;
  left: 30px;
  width: 60px;
  transform-origin: 0 50%;
  border-radius: 0 100px 100px 0;
  animation: rotate-circle 4.25s ease-in;
}

.check-icon::before,
.check-icon::after {
  content: '';
  height: 100px;
  position: absolute;
  background: #fff;
  transform: rotate(-45deg);
}

.icon-line {
  height: 5px;
  background-color: #4caf50;
  display: block;
  border-radius: 2px;
  position: absolute;
  z-index: 10;
}

.icon-line.line-tip {
  top: 46px;
  left: 14px;
  width: 25px;
  transform: rotate(45deg);
  animation: icon-line-tip 0.75s;
}

.icon-line.line-long {
  top: 38px;
  right: 8px;
  width: 47px;
  transform: rotate(-45deg);
  animation: icon-line-long 0.75s;
}

.icon-circle {
  top: -4px;
  left: -4px;
  z-index: 10;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  position: absolute;
  box-sizing: content-box;
  border: 4px solid rgba(76, 175, 80, 0.5);
}

.icon-fix {
  top: 8px;
  width: 5px;
  left: 26px;
  z-index: 1;
  height: 85px;
  position: absolute;
  transform: rotate(-45deg);
  background-color: #fff;
}

.success-text {
  font-size: 24px;
  font-weight: bold;
  color: #4caf50;
  margin-top: 20px;
}

@keyframes rotate-circle {
  0% {
    transform: rotate(-45deg);
  }

  5% {
    transform: rotate(-45deg);
  }

  12% {
    transform: rotate(-405deg);
  }

  100% {
    transform: rotate(-405deg);
  }
}

@keyframes icon-line-tip {
  0% {
    width: 0;
    left: 1px;
    top: 19px;
  }

  54% {
    width: 0;
    left: 1px;
    top: 19px;
  }

  70% {
    width: 50px;
    left: -8px;
    top: 37px;
  }

  84% {
    width: 17px;
    left: 21px;
    top: 48px;
  }

  100% {
    width: 25px;
    left: 14px;
    top: 45px;
  }
}

@keyframes icon-line-long {
  0% {
    width: 0;
    right: 46px;
    top: 54px;
  }

  65% {
    width: 0;
    right: 46px;
    top: 54px;
  }

  84% {
    width: 55px;
    right: 0px;
    top: 35px;
  }

  100% {
    width: 47px;
    right: 8px;
    top: 38px;
  }
}
</style>
