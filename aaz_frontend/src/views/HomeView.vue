<template>
  <v-app>
    <div class="background-layer page-background"></div>
    <!-- 左側導覽欄 -->
    <div class="sidebar">
      <!-- Logo 和標題 -->
      <div class="sidebar-header">
          <h1 class="logo-title d-flex align-center justify-center mb-6">
            欸
            <img src="/AAZ_icon.png" alt="A" class="logo-icon" />
            誌
          </h1>
        <div class="sidebar-subtitle text-h6 font-weight-bold mt-4">我的行程</div>
      </div>

      <!-- 用戶資訊 -->
      <div class="sidebar-user">
        <v-menu>
          <template v-slot:activator="{ props }">
            <div class="user-profile" v-bind="props">
              <v-avatar color="white" size="48">
                <span class="text-primary text-h6">{{ userInitial }}</span>
              </v-avatar>
              <div class="ml-3">
                <div class="font-weight-bold">{{ userStore.nickname }}</div>
                <div class="text-caption text-grey">點擊查看選單</div>
              </div>
            </div>
          </template>
          <v-list>
            <v-list-item @click="logout">
              <template v-slot:prepend>
                <v-icon>mdi-logout</v-icon>
              </template>
              <v-list-item-title>登出</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>

      <!-- 新增群組按鈕 -->
      <div class="sidebar-actions">
        <v-btn
          color="primary"
          size="large"
          prepend-icon="mdi-plus"
          @click="showCreateDialog = true"
          block
        >
          新增群組
        </v-btn>
      </div>
    </div>

    <!-- 右側內容區 -->
    <v-main class="main-content">
      <!-- 主要內容 -->
      <div class="content-wrapper-home">
        <v-container class="pa-6">
          <!-- 群組卡片列表 -->
          <v-row v-if="!loading" class="mt-4">
            <v-col v-for="group in groups" :key="group.id" cols="12" md="6" lg="4">
              <v-card hover @click="goToGroup(group.id)" class="group-card" elevation="3">
                <v-img
                  :src="`https://picsum.photos/seed/${group.id}/400/200`"
                  height="200"
                  cover
                  gradient="to bottom, rgba(0,0,0,.1), rgba(0,0,0,.5)"
                >
                  <v-card-title class="text-white text-h5 font-weight-bold">
                    {{ group.name }}
                  </v-card-title>
                </v-img>

                <v-card-text>
                  <div class="mb-2">
                    <v-icon size="small" class="mr-1">mdi-calendar</v-icon>
                    {{ formatDate(group.startDate) }} - {{ formatDate(group.endDate) }}
                  </div>
                  <div class="d-flex align-center">
                    <v-chip color="info" size="small">
                      <v-icon start>mdi-account-group</v-icon>
                      {{ group.members?.length || 0 }} 人
                    </v-chip>
                    <v-chip color="success" size="small">
                      <v-icon start>mdi-currency-usd</v-icon>
                      {{ group.baseCurrency }}
                    </v-chip>
                  </div>
                </v-card-text>
              </v-card>
            </v-col>
          </v-row>

          <!-- Loading -->
          <v-row v-else>
            <v-col class="text-center">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </v-col>
          </v-row>

          <!-- 空狀態 -->
          <v-row v-if="!loading && groups.length === 0">
            <v-col class="text-center py-16">
              <v-icon size="120" color="grey-lighten-2">mdi-bag-suitcase-outline</v-icon>
              <p class="text-h5 mt-4 text-grey">還沒有安排行程？</p>
              <p class="text-body-1 text-grey">點擊右上方按鈕建立你的第一個旅遊行程！</p>
            </v-col>
          </v-row>
        </v-container>
      </div>
      <!-- 關閉 content-wrapper-home -->

      <!-- 新增群組對話框 -->
      <v-dialog v-model="showCreateDialog" max-width="600">
        <v-card>
          <v-card-title class="bg-primary text-white"> 新增行程 </v-card-title>
          <v-card-text class="pt-4">
            <v-form ref="newGroupForm" v-model="newGroupFormValid">
              <v-text-field
                v-model="newGroup.name"
                label="行程名稱"
                prepend-icon="mdi-bag-suitcase"
                :rules="[(v) => !!v || '請輸入行程名稱']"
                required
              ></v-text-field>

              <div class="d-flex align-center mt-4">
                <span v-if="newGroupDateError" class="text-red text-caption ml-2">{{
                  newGroupDateError
                }}</span>
              </div>
              <v-text-field
                v-model="newGroup.startDate"
                label="開始日期"
                type="date"
                prepend-icon="mdi-calendar-start"
                :error="!!newGroupDateError"
                required
              ></v-text-field>

              <v-text-field
                v-model="newGroup.endDate"
                label="結束日期"
                type="date"
                prepend-icon="mdi-calendar-end"
                :error="!!newGroupDateError"
                required
              ></v-text-field>

              <v-select
                v-model="newGroup.baseCurrency"
                label="主要使用貨幣"
                prepend-icon="mdi-currency-usd"
                :items="currencies"
                required
              ></v-select>

              <!-- 自訂貨幣輸入框（當選擇"其他"時顯示） -->
              <v-text-field
                v-if="newGroup.baseCurrency === '其他'"
                v-model="newGroup.customCurrency"
                label="請輸入貨幣代碼"
                prepend-icon="mdi-currency-usd"
                placeholder="例如：VND, PHP, MYR"
                :rules="[(v) => !!v || '請輸入貨幣代碼']"
                required
                class="mt-3"
              ></v-text-field>

              <!-- 新增成員區塊 -->
              <div class="mt-4">
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <v-icon class="mr-2">mdi-account-multiple</v-icon>
                    <span class="text-subtitle-1 font-weight-medium">成員列表</span>
                  </div>
                  <v-btn
                    prepend-icon="mdi-account-plus"
                    color="secondary"
                    variant="tonal"
                    size="small"
                    @click="addNewMemberField"
                    :disabled="isAddMemberDisabled"
                  >
                    新增成員
                  </v-btn>
                </div>
                <!-- 動態新增的成員輸入框 -->
                <div v-for="(member, index) in newMembers" :key="member.tempId" class="mt-3">
                  <v-text-field
                    v-model="member.name"
                    placeholder="輸入成員名稱"
                    prepend-icon="mdi-account"
                    density="compact"
                    hide-details
                    @blur="handleNewMemberBlur(member)"
                    :ref="
                      (el) => {
                        if (index === newMembers.length - 1) latestMemberInput = el;
                      }
                    "
                  ></v-text-field>
                </div>
              </div>

              <v-textarea
                v-model="newGroup.announcement"
                label="公告欄"
                prepend-icon="mdi-bullhorn"
                rows="3"
                class="mt-4"
              ></v-textarea>
            </v-form>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn @click="showCreateDialog = false">取消</v-btn>
            <v-btn
              color="primary"
              @click="showConfirmCreateDialog"
              :disabled="!newGroupFormValid || !!newGroupDateError"
              >建立</v-btn
            >
          </v-card-actions>
        </v-card>
      </v-dialog>

      <!-- 確認建立群組對話框 -->
      <v-dialog v-model="showConfirmDialog" max-width="400px">
        <v-card>
          <v-card-title class="text-h6">確認建立</v-card-title>
          <v-card-text>
            幣別經儲存後無法修改，確定嗎？
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn variant="text" @click="showConfirmDialog = false">取消</v-btn>
            <v-btn color="primary" variant="elevated" @click="confirmCreateGroup">
              確定
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-main>
  </v-app>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';

const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const groups = ref([]);
const showCreateDialog = ref(false);
const showConfirmDialog = ref(false);
const newGroupForm = ref(null);
const newGroupFormValid = ref(true);

const newGroup = ref({
  name: '',
  startDate: '',
  endDate: '',
  baseCurrency: 'TWD',
  customCurrency: '',
  announcement: '',
  members: [],
});

const newMembers = ref([]); // 用於動態新增的成員欄位
const latestMemberInput = ref(null); // 用於存儲最新輸入框的引用

const currencies = [
  'TWD',
  'JPY',
  'USD',
  'EUR',
  'CNY',
  'GBP',
  'KRW',
  'THB',
  'SGD',
  'HKD',
  'AUD',
  '其他',
];

// 日期驗證錯誤訊息
const newGroupDateError = computed(() => {
  if (
    newGroup.value.startDate &&
    newGroup.value.endDate &&
    newGroup.value.startDate > newGroup.value.endDate
  ) {
    return '起始日不得晚於結束日';
  }
  return '';
});

const userInitial = computed(() => {
  return userStore.nickname ? userStore.nickname.charAt(0).toUpperCase() : 'U';
});

// 判斷「新增成員」按鈕是否應禁用
const isAddMemberDisabled = computed(() => {
  if (newMembers.value.length === 0) {
    return false; // 如果沒有新成員欄位，則不禁用
  }
  const lastMember = newMembers.value[newMembers.value.length - 1];
  return lastMember.name.trim() === ''; // 如果最後一個欄位是空的，則禁用
});

// 處理新成員輸入框失焦事件
const handleNewMemberBlur = (member) => {
  if (member.name.trim() === '') {
    newMembers.value = newMembers.value.filter((m) => m.tempId !== member.tempId);
  }
};

// 新增一個空的成員輸入欄位
const addNewMemberField = async () => {
  newMembers.value.push({ tempId: Date.now(), name: '' });
  // 等待 DOM 更新後，讓新輸入框獲得焦點
  await nextTick();
  if (latestMemberInput.value) {
    latestMemberInput.value.focus();
  }
};

// 監聽對話框關閉，重置成員列表
watch(showCreateDialog, (newVal) => {
  if (!newVal) {
    // 當對話框關閉時，重置成員列表
    newMembers.value = [];
  }
});

onMounted(async () => {
  await loadGroups();
});

async function loadGroups() {
  try {
    loading.value = true;
    const response = await axios.get('/trips');
    groups.value = response.data;
  } catch (error) {
    console.error('載入群組失敗:', error);
  } finally {
    loading.value = false;
  }
}

// 顯示確認對話框
async function showConfirmCreateDialog() {
  // 驗證表單
  if (newGroupForm.value) {
    const { valid } = await newGroupForm.value.validate();
    if (!valid) return;
  }

  // 檢查日期驗證
  if (newGroupDateError.value) {
    return;
  }

  // 顯示確認對話框
  showConfirmDialog.value = true;
}

// 確認建立群組
async function confirmCreateGroup() {
  showConfirmDialog.value = false;
  await createGroup();
}

async function createGroup() {

  try {
    // 過濾掉未填寫姓名的新成員
    const actualNewMembers = newMembers.value
      .map((m) => ({ displayName: m.name.trim() }))
      .filter((m) => m.displayName !== '');

    // 將有效成員添加到 newGroup.members
    newGroup.value.members = actualNewMembers;

    // 如果選擇"其他"，使用自訂貨幣代碼
    const groupData = { ...newGroup.value };
    if (groupData.baseCurrency === '其他') {
      groupData.baseCurrency = groupData.customCurrency.trim().toUpperCase();
    }
    delete groupData.customCurrency; // 移除 customCurrency 欄位

    await axios.post('/trips', groupData);
    showCreateDialog.value = false;
    await loadGroups();
    // 重置表單
    newGroup.value = {
      name: '',
      startDate: '',
      endDate: '',
      baseCurrency: 'TWD',
      customCurrency: '',
      announcement: '',
      members: [],
    };
    newMembers.value = []; // 重置成員列表
  } catch (error) {
    console.error('建立群組失敗:', error);
    alert('建立失敗：' + error.message);
  }
}

function goToGroup(groupId) {
  router.push(`/groups/${groupId}`);
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleDateString('zh-TW');
}

function logout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
/* ========== 左側導覽欄樣式 ========== */
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  width: 20%;
  height: 100vh;
  background: rgba(252, 251, 247, 0.6);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 2; /* 確保側邊欄在最上層 */
  display: flex;
  flex-direction: column;
  padding: 32px 24px;
}

.sidebar-header {
  text-align: center;
  padding-bottom: 32px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.sidebar-logo {
  color: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-subtitle {
  color: #333;
}

.logo-icon {
  position: relative;
  top: -2px;
  height: 1.3em;
  margin: 0 0.15em;
  vertical-align: middle;
}

.sidebar-user {
  margin-top: 32px;
  padding-bottom: 32px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.user-profile {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-profile:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

.sidebar-actions {
  margin-top: auto;
  padding-top: 32px;
}

/* ========== 右側內容區樣式 ========== */
.main-content {
  position: relative; /* 創建堆疊上下文 */
  z-index: 1; /* 確保內容在背景層之上 */
  margin-left: 20%;
  width: 80%;
  min-height: 100vh;
  padding: 0;
}

.content-wrapper-home {
  position: relative;
  z-index: 1;
  width: 100% !important;
  max-width: 100% !important;
  margin: 0 !important;
  background-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  min-height: 100vh;
  padding: 32px 0;
}

/* ========== 群組卡片樣式 ========== */
.group-card {
  transition: transform 0.2s;
  cursor: pointer;
}

.group-card:hover {
  transform: translateY(-4px);
}

/* 手機版響應式 */
@media (max-width: 768px) {
  .sidebar {
    width: 80px;
  }

  .main-content {
    margin-left: 80px;
    width: calc(100% - 80px);
  }

  .sidebar-subtitle,
  .user-profile div {
    display: none;
  }
}
</style>
