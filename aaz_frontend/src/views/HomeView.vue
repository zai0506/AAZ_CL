<template>
  <v-app>
    <!-- 左側導覽欄 -->
    <div class="sidebar">
      <!-- Logo -->
      <div class="sidebar-header">
        <h1 class="logo-title d-flex align-center justify-center mb-6" @click="handleLogoClick"
          style="cursor: pointer;">
          欸
          <img src="/AAZ_icon.png" alt="A" class="logo-icon" />
          誌
        </h1>
      </div>

      <!-- 固定的"我的行程"標題 -->
      <div class="nav-header">
        <v-list class="pa-0" bg-color="transparent">
          <v-list-subheader>我的行程</v-list-subheader>
          <v-divider class="my-3"></v-divider>
        </v-list>
      </div>

      <!-- 可滾動的行程列表區域 -->
      <div class="nav-content">
        <v-list class="pa-0" bg-color="transparent">
          <!-- 行程群組列表 -->
          <v-list-item v-for="group in sortedGroups" :key="group.id" class="nav-item" @click="goToGroup(group.id)"
            @mouseenter="hoveredGroupId = group.id" @mouseleave="hoveredGroupId = null">
            <template v-slot:prepend>
              <v-icon>mdi-beach</v-icon>
            </template>
            <v-list-item-title>{{ group.name }}</v-list-item-title>
          </v-list-item>

          <!-- 新增群組項目 -->
          <v-list-item prepend-icon="mdi-plus-circle-outline" title="新增行程" @click="showCreateDialog = true"
            class="nav-item nav-item-add mt-4"></v-list-item>
        </v-list>
      </div>

      <!-- 底部使用者區 -->
      <v-menu location="top" v-model="menuOpen">
        <template v-slot:activator="{ props }">
          <v-list-item class="nav-item" v-bind="props" value="user-menu">
            <template v-slot:prepend>
              <v-avatar color="rgb(85, 214, 194)" size="40">
                <v-icon color="white" size="28">mdi-robot-dead</v-icon>
              </v-avatar>
            </template>
            <v-list-item-title class="font-weight-bold">{{ userStore.nickname }}</v-list-item-title>
            <template v-slot:append>
              <v-icon :icon="menuOpen ? 'mdi-chevron-down' : 'mdi-chevron-up'"></v-icon>
            </template>
          </v-list-item>
        </template>

        <v-list class="submenu-list">
          <v-list-item value="profile" prepend-icon="mdi-account-circle-outline"
            @click="profileDialogStore.openDialog('mdi-robot-dead')">
            <v-list-item-title>個人檔案</v-list-item-title>
          </v-list-item>
          <v-list-item value="logout" @click="logout" prepend-icon="mdi-logout">
            <v-list-item-title>登出</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </div>

    <!-- 右側內容區 -->
    <v-main class="main-content">
      <!-- 主要內容 -->
      <div class="content-wrapper-home">
        <v-container class="pa-6">
          <!-- 群組卡片列表 -->
          <v-row v-if="!loading">
            <v-col v-for="group in groups" :key="group.id" cols="12" md="6" lg="4">
              <v-card hover @click="goToGroup(group.id)" class="group-card" elevation="3"
                :class="{ 'is-hovered': hoveredGroupId === group.id }">
                <!-- 群組圖片 -->
                <v-img :src="`https://picsum.photos/id/${[
                  '1016', // 山脈與都市 (適合日本/瑞士)
                  '1039', // 森林小徑 (適合健行)
                  '1043', // 歐洲街景
                  '1050', // 現代建築
                  '1067', // 都市夜景
                  '1080', // 草原大地 (適合澳洲)
                  '10',   // 海邊風景
                  '230',  // 懷舊街道
                  '249',  // 橋樑與都市
                  '274'   // 歐洲風格建築
                ][Math.abs(parseInt(group.id || 0)) % 10]
                  }/800/400`" height="180" cover>
                  <div class="group-overlay-content">
                    <!-- 群組名稱 (輕巧活潑樣式) -->
                    <div class="group-name-header-light">
                      <h3 class="mb-0">{{ group.name }}</h3>
                    </div>
                  </div>
                </v-img>

                <v-card-text class="pa-4">

                  <!-- 日期 -->
                  <div class="mb-3">
                    <v-icon size="small" class="mr-1">mdi-calendar</v-icon>
                    <span class="text-body-2">{{ formatDate(group.startDate) }} - {{ formatDate(group.endDate) }}</span>
                  </div>

                  <!-- 底部資訊 -->
                  <div class="d-flex align-center justify-space-between">
                    <!-- 貨幣 (黑金色調) -->
                    <v-chip size="small" variant="flat" class="currency-chip-gold">
                      <v-icon start size="small">mdi-currency-usd</v-icon>
                      {{ group.baseCurrency }}
                    </v-chip>

                    <!-- 成員圖示 -->
                    <div class="d-flex align-center">
                      <span class="text-body-2 mr-2">{{ group.members?.length || 0 }} 人</span>
                      <div class="member-icons-container">
                        <v-avatar v-for="(member, index) in group.members?.slice(0, 5)" :key="member.id"
                          :color="member.isCreator ? 'rgb(85, 214, 194)' : 'grey-lighten-1'" size="28"
                          :style="{ marginLeft: index > 0 ? '-8px' : '0', zIndex: 5 - index }"
                          class="member-icon-avatar">
                          <v-icon color="white" size="16">{{ getMemberIcon(member, group) }}</v-icon>
                        </v-avatar>
                        <v-avatar v-if="(group.members?.length || 0) > 5" color="grey-darken-1" size="28"
                          style="margin-left: -8px; z-index: 0" class="member-icon-avatar">
                          <span class="text-caption text-white">+{{ (group.members?.length || 0) - 5 }}</span>
                        </v-avatar>
                      </div>
                    </div>
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
        <v-card class="dialog-card">
          <v-card-title class="dialog-sticky-header text-white" style="background-color: #5470C6;"> 新增行程
          </v-card-title>
          <v-card-text class="dialog-scrollable-content pt-4">
            <v-form ref="newGroupForm" v-model="newGroupFormValid">
              <v-text-field v-model="newGroup.name" prepend-icon="mdi-bag-suitcase" :rules="[(v) => !!v || '此欄位為必填']"
                required>
                <template v-slot:label>
                  行程名稱 <span class="text-red">*</span>
                </template>
              </v-text-field>

              <div class="d-flex align-center mt-4">
                <span v-if="newGroupDateError" class="text-red text-caption ml-2">{{
                  newGroupDateError
                  }}</span>
              </div>
              <v-text-field v-model="newGroup.startDate" type="date" prepend-icon="mdi-calendar-start"
                :error="!!newGroupDateError" :rules="[(v) => !!v || '此欄位為必填']" required>
                <template v-slot:label>
                  開始日期 <span class="text-red">*</span>
                </template>
              </v-text-field>

              <v-text-field v-model="newGroup.endDate" type="date" prepend-icon="mdi-calendar-end"
                :error="!!newGroupDateError" :rules="[(v) => !!v || '此欄位為必填']" required>
                <template v-slot:label>
                  結束日期 <span class="text-red">*</span>
                </template>
              </v-text-field>

              <v-select v-model="newGroup.baseCurrency" prepend-icon="mdi-currency-usd" :items="currencies"
                :rules="[(v) => !!v || '此欄位為必填']" required>
                <template v-slot:label>
                  主要使用貨幣 <span class="text-red">*</span>
                </template>
              </v-select>

              <!-- 自訂貨幣輸入框（當選擇"其他"時顯示） -->
              <v-text-field v-if="newGroup.baseCurrency === '其他'" v-model="newGroup.customCurrency" label="請輸入貨幣代碼"
                prepend-icon="mdi-currency-usd" placeholder="例如：VND, PHP, MYR" :rules="[(v) => !!v || '請輸入貨幣代碼']"
                required class="mt-3"></v-text-field>

              <!-- 新增成員區塊 -->
              <div class="mt-4">
                <div class="d-flex align-center justify-space-between mb-2">
                  <div class="d-flex align-center">
                    <v-icon class="mr-2">mdi-account-multiple</v-icon>
                    <span class="text-subtitle-1 font-weight-medium">成員列表</span>
                  </div>
                  <v-btn prepend-icon="mdi-account-plus" color="secondary" variant="tonal" size="small"
                    @click="addNewMemberField" :disabled="isAddMemberDisabled">
                    新增成員
                  </v-btn>
                </div>
                <!-- 動態新增的成員輸入框 -->
                <div v-for="(member, index) in newMembers" :key="member.tempId" class="mt-3">
                  <v-text-field v-model="member.name" placeholder="輸入成員名稱" prepend-icon="mdi-account" density="compact"
                    hide-details @blur="handleNewMemberBlur(member)" :ref="(el) => {
                      if (index === newMembers.length - 1) latestMemberInput = el;
                    }
                      "></v-text-field>
                </div>
              </div>

              <v-textarea v-model="newGroup.announcement" label="公告欄" prepend-icon="mdi-bullhorn" rows="3"
                class="mt-4"></v-textarea>

              <!-- 錯誤訊息提示 -->
              <v-alert v-if="createGroupErrorMessage" type="error" density="compact" class="mt-4">
                {{ createGroupErrorMessage }}
              </v-alert>
            </v-form>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn @click="showCreateDialog = false">取消</v-btn>
            <v-btn color="#5470C6" class="text-white" variant="elevated" @click="showConfirmCreateDialog"
              :disabled="!newGroupFormValid || !!newGroupDateError">建立</v-btn>
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
            <v-btn color="#5470C6" class="text-white" variant="elevated" @click="confirmCreateGroup">
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
import { useProfileDialogStore } from '@/stores/profileDialog';
import axios from '@/api/axios';

const router = useRouter();
const userStore = useUserStore();
const profileDialogStore = useProfileDialogStore();

const loading = ref(true);
const groups = ref([]);
const showCreateDialog = ref(false);
const showConfirmDialog = ref(false);
const newGroupForm = ref(null);
const newGroupFormValid = ref(true);
const menuOpen = ref(false);
const hoveredGroupId = ref(null); // 用於同步 hover 效果

const sortedGroups = computed(() =>
  [...groups.value].sort((a, b) => a.id - b.id) // 依照 ID 由舊到新排序
);

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

// 成員 ICON 列表（與 GroupDetailView 保持一致）
const memberIcons = [
  'mdi-robot-dead',
  'mdi-ghost',
  'mdi-alien',
  'mdi-emoticon-poop',
  'mdi-cat',
  'mdi-dog',
  'mdi-panda',
  'mdi-elephant',
  'mdi-koala',
  'mdi-penguin',
  'mdi-duck',
  'mdi-owl',
  'mdi-pig-variant',
  'mdi-rabbit-variant',
  'mdi-fish',
  'mdi-rodent'
];

// 根據成員 ID 獲取對應的 ICON（按成員 ID 排序後依序分配）
const getMemberIcon = (member, group) => {
  if (!member || !group?.members) return 'mdi-account';

  // 將所有成員按 ID 排序
  const sortedMembers = [...group.members].sort((a, b) => a.id - b.id);

  // 找到該成員在排序後的位置
  const memberIndex = sortedMembers.findIndex(m => m.id === member.id);

  if (memberIndex === -1) return 'mdi-account';

  // 根據位置分配 icon（如果成員數超過 icon 數量，會循環使用）
  const iconIndex = memberIndex % memberIcons.length;
  return memberIcons[iconIndex];
};

// 判斷「新增成員」按鈕是否應禁用
const isAddMemberDisabled = computed(() => {
  if (newMembers.value.length === 0) {
    return false; // 如果沒有新成員欄位，則不禁用
  }
  const lastMember = newMembers.value[newMembers.value.length - 1];
  return lastMember.name.trim() === ''; // 如果最後一個欄位是空的，則禁用
});

function handleLogoClick() {
  window.location.reload();
}
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

const createGroupErrorMessage = ref(''); // 用於顯示建立群組的錯誤訊息

// 監聽對話框關閉，重置成員列表和錯誤訊息
watch(showCreateDialog, (newVal) => {
  if (!newVal) {
    // 當對話框關閉時，重置成員列表和錯誤訊息
    newMembers.value = [];
    createGroupErrorMessage.value = '';
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
  createGroupErrorMessage.value = ''; // 先清空舊的錯誤訊息

  // 驗證表單
  if (newGroupForm.value) {
    const { valid } = await newGroupForm.value.validate();
    if (!valid) {
      createGroupErrorMessage.value = '請填寫所有必填欄位';
      return;
    }
  }

  // 檢查日期驗證
  if (newGroupDateError.value) {
    createGroupErrorMessage.value = '起始日不得晚於結束日';
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
    // 在這裡也可以設定後端回傳的錯誤訊息
    createGroupErrorMessage.value = error.response?.data?.message || '建立失敗，請稍後再試';
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
  z-index: 2;
  /* 確保側邊欄在最上層 */
  display: flex;
  flex-direction: column;
  padding: 32px 24px;
}

.sidebar-header {
  text-align: center;
  padding-bottom: 24px;
  /* 調整間距 */
  margin-bottom: 12px;
  /* 調整間距以匹配下方 */
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

.logo-title {
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.logo-icon {
  position: relative;
  top: -2px;
  height: 1.3em;
  margin: 0 0.15em;
  vertical-align: middle;
}

/* 固定的導覽標題區域 */
.nav-header {
  flex-shrink: 0;
}

/* 可滾動的導覽內容區域 */
.nav-content {
  flex: 1 1 auto;
  overflow-y: auto;
  overflow-x: hidden;
  margin-right: -8px;
  padding-right: 8px;
  min-height: 0;
}

/* 固定底部使用者區域 */
.sidebar>.v-menu {
  flex-shrink: 0;
}

/* 隱藏滾動條但保持滾動功能 */
.nav-content::-webkit-scrollbar {
  width: 6px;
}

.nav-content::-webkit-scrollbar-track {
  background: transparent;
}

.nav-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.nav-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* 新增導覽項目樣式 */
.nav-item {
  color: #333;
  border-radius: 8px;
  margin: 4px 0;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 覆寫 Vuetify 預設樣式，讓 v-list-subheader 和 v-list-item 背景透明 */
.v-list-subheader {
  color: #5470C6;
  font-weight: bold;
  padding-left: 16px;
  font-size: 1.6em;
  /* 我的行程字體再放大 */
  padding-top: 8px;
  /* 增加頂部內邊距 */
  padding-bottom: 8px;
  /* 增加底部內邊距 */
  line-height: 1.2;
  /* 調整行高以確保垂直居中 */
}

.v-list-item {
  border-radius: 8px !important;
  font-size: 1.4em;
  /* 其他選單字體再放大 */
}

.nav-item:hover {
  background-color: #000000 !important;
  /* HOVER時框內顏色 */
  color: #FFD700 !important;
  /* HOVER時字體變白色 */
}

/* 確保 hover 時內部的 icon 和 title 也變色 */
.nav-item:hover .v-icon,
.nav-item:hover .v-list-item-title {
  color: #FFD700 !important;
}

/* hover 時確保新增群組項目也變白 */
.nav-item-add:hover .v-icon,
.nav-item-add:hover .v-list-item-title,
.nav-item-add:hover :deep(.v-list-item__prepend .v-icon),
.nav-item-add:hover :deep(.v-list-item__content) {
  color: #FFD700 !important;
}

/* 新增群組項目的預設顏色 */
.nav-item.nav-item-add .v-icon {
  color: #5470C6 !important;
}

.nav-item.nav-item-add .v-list-item-title {
  color: #5470C6 !important;
  font-weight: 700 !important;
}

.nav-item.nav-item-add {
  color: #5470C6 !important;
}

/* 確保更深層的元素也變色和加粗 */
.nav-item-add :deep(.v-list-item__prepend .v-icon) {
  color: #5470C6 !important;
}

.nav-item-add :deep(.v-list-item__content) {
  color: #5470C6 !important;
  font-weight: 700 !important;
}

.nav-item-add :deep(.v-list-item__content .v-list-item-title) {
  font-weight: 700 !important;
}

/* 讓使用者選單的觸發器有手型游標 */
.nav-item[value='user-menu'] {
  cursor: pointer;
}

/* 彈出式子選單的樣式 */
.submenu-list {
  background-color: rgba(240, 240, 240, 0.7) !important;
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
  border-radius: 8px !important;
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 4px !important;
}

.submenu-list .v-list-item:hover {
  background-color: rgb(85, 214, 194) !important;
  color: white !important;
}

.submenu-list .v-list-item:hover .v-list-item-title,
.submenu-list .v-list-item:hover .v-icon {
  color: white !important;
}

/* ========== Dialog Styles ========== */
.dialog-card {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  /* 限制對話框最大高度 */
}

.dialog-sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
}

.dialog-scrollable-content {
  overflow-y: auto;
  /* 當內容超出時，只讓這部分滾動 */
}

/* ========== 右側內容區樣式 ========== */
.main-content {
  position: relative;
  /* 創建堆疊上下文 */
  z-index: 1;
  /* 確保內容在背景層之上 */
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
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  border: 3px dashed #FFD700 !important;
  border-radius: 16px !important;
  overflow: hidden;
  background-color: #ffffff !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
}

.group-card:hover,
.group-card.is-hovered {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25) !important;
}

/* 圖片上的內容容器 (定位群組名) */
.group-overlay-content {
  position: absolute;
  top: 16px;
  left: 20px;
  right: 20px;
  max-height: 90px;
  z-index: 1;
}

/* 群組名稱標題樣式 (純文字) */
.group-name-header-light {
  display: block;
  padding: 4px 8px;
  max-width: 100%;
}

.group-name-header-light h3 {
  color: #ffffff;
  text-shadow:
    0 2px 4px rgba(0, 0, 0, 0.6),
    0 1px 2px rgba(0, 0, 0, 0.8);
  font-weight: 900;
  font-size: 2rem;
  line-height: 1.4;
  word-wrap: break-word;
  overflow-wrap: break-word;
  max-height: 90px;
  overflow: hidden;
}

/* 公告欄對話框樣式 (輕巧活潑風格) */
.announcement-bubble-light {
  background: rgba(255, 248, 220, 0.95) !important;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 184, 0, 0.6) !important;
  color: #6b5200 !important;
  padding: 14px 16px;
  border-radius: 12px;
  position: relative;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  width: 85%;
  max-width: 280px;
  min-height: 70px;
  display: flex;
  align-items: center;
}

.announcement-bubble-light::before {
  content: '';
  position: absolute;
  left: 20px;
  top: -10px;
  width: 18px;
  height: 18px;
  background: rgba(255, 248, 220, 0.95) !important;
  border-left: 2px solid rgba(255, 184, 0, 0.6) !important;
  border-top: 2px solid rgba(255, 184, 0, 0.6) !important;
  transform: rotate(45deg);
  border-radius: 3px 0 0 0;
  z-index: 2;
}

.announcement-bubble-light::after {
  content: '';
  position: absolute;
  left: 18px;
  top: -2px;
  width: 22px;
  height: 4px;
  background: rgba(255, 248, 220, 0.95);
  z-index: 1;
}

.announcement-bubble-light p {
  color: #6b5200 !important;
  line-height: 1.6;
  position: relative;
  z-index: 2;
}

/* 貨幣 chip 黑金色調 */
.currency-chip-gold {
  background: linear-gradient(135deg, #1a1a1a 0%, #000000 100%) !important;
  color: #FFD700 !important;
  border: 1px solid #FFD700;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
}

.currency-chip-gold .v-icon {
  color: #FFD700 !important;
}

/* 成員圖示容器 */
.member-icons-container {
  display: flex;
  align-items: center;
}

.member-icon-avatar {
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
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