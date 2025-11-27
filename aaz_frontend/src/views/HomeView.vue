<template>
  <v-container fluid class="bg-grey-lighten-4 min-vh-100">
    <!-- 頂部導航 -->
    <v-app-bar color="primary" dark elevation="2">
      <v-toolbar-title class="text-h5 font-weight-bold"> AAZ 旅遊分帳 </v-toolbar-title>
      <v-spacer></v-spacer>
      <v-menu>
        <template v-slot:activator="{ props }">
          <v-btn icon v-bind="props">
            <v-avatar color="white" size="36">
              <span class="text-primary">{{ userInitial }}</span>
            </v-avatar>
          </v-btn>
        </template>
        <v-list>
          <v-list-item>
            <v-list-item-title>{{ userStore.nickname }}</v-list-item-title>
          </v-list-item>
          <v-divider></v-divider>
          <v-list-item @click="logout">
            <v-list-item-title>登出</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>

    <!-- 主要內容 -->
    <v-container class="mt-8">
      <!-- 標題與新增按鈕 -->
      <v-row class="mb-4">
        <v-col>
          <h1 class="text-h4 font-weight-bold">我的群組</h1>
        </v-col>
        <v-col class="text-right">
          <v-btn
            color="primary"
            size="large"
            prepend-icon="mdi-plus"
            @click="showCreateDialog = true"
          >
            新增群組
          </v-btn>
        </v-col>
      </v-row>

      <!-- 群組卡片列表 -->
      <v-row v-if="!loading">
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
              <div class="d-flex justify-space-between align-center">
                <v-chip color="success" size="small">
                  <v-icon start>mdi-currency-usd</v-icon>
                  {{ group.baseCurrency }}
                </v-chip>
                <v-chip color="info" size="small">
                  <v-icon start>mdi-account-group</v-icon>
                  {{ group.members?.length || 0 }} 人
                </v-chip>
              </div>
            </v-card-text>

            <v-card-actions>
              <v-btn color="primary" variant="text" @click.stop="goToGroup(group.id)">
                查看詳情
              </v-btn>
              <v-spacer></v-spacer>
              <v-btn icon size="small">
                <v-icon>mdi-dots-vertical</v-icon>
              </v-btn>
            </v-card-actions>
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
          <p class="text-h5 mt-4 text-grey">還沒有群組</p>
          <p class="text-body-1 text-grey">點擊上方按鈕建立你的第一個旅遊群組！</p>
        </v-col>
      </v-row>
    </v-container>

    <!-- 新增群組對話框 -->
    <v-dialog v-model="showCreateDialog" max-width="600">
      <v-card>
        <v-card-title class="bg-primary text-white"> 新增旅遊群組 </v-card-title>
        <v-card-text class="pt-4">
          <v-form ref="form">
            <v-text-field
              v-model="newGroup.name"
              label="群組名稱"
              prepend-icon="mdi-bag-suitcase"
              :rules="[(v) => !!v || '請輸入群組名稱']"
              required
            ></v-text-field>

            <v-text-field
              v-model="newGroup.startDate"
              label="開始日期"
              type="date"
              prepend-icon="mdi-calendar-start"
              :rules="[(v) => !!v || '請選擇開始日期']"
              required
            ></v-text-field>

            <v-text-field
              v-model="newGroup.endDate"
              label="結束日期"
              type="date"
              prepend-icon="mdi-calendar-end"
              :rules="[(v) => !!v || '請選擇結束日期']"
              required
            ></v-text-field>

            <v-select
              v-model="newGroup.baseCurrency"
              label="主要使用貨幣"
              prepend-icon="mdi-currency-usd"
              :items="currencies"
              required
            ></v-select>

            <v-textarea
              v-model="newGroup.announcement"
              label="公告欄"
              prepend-icon="mdi-bullhorn"
              rows="3"
            ></v-textarea>
          </v-form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn @click="showCreateDialog = false">取消</v-btn>
          <v-btn color="primary" @click="createGroup">建立</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';

const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const groups = ref([]);
const showCreateDialog = ref(false);
const form = ref(null);

const newGroup = ref({
  name: '',
  startDate: '',
  endDate: '',
  baseCurrency: 'TWD',
  announcement: '',
  members: [],
});

const currencies = ['TWD', 'USD', 'JPY', 'CNY', 'EUR', 'KRW'];

const userInitial = computed(() => {
  return userStore.nickname ? userStore.nickname.charAt(0).toUpperCase() : 'U';
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

async function createGroup() {
  const { valid } = await form.value.validate();
  if (!valid) return;

  try {
    await axios.post('/trips', newGroup.value);
    showCreateDialog.value = false;
    await loadGroups();
    // 重置表單
    newGroup.value = {
      name: '',
      startDate: '',
      endDate: '',
      baseCurrency: 'TWD',
      announcement: '',
      members: [],
    };
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
.group-card {
  transition: transform 0.2s;
  cursor: pointer;
}

.group-card:hover {
  transform: translateY(-4px);
}

.min-vh-100 {
  min-height: 100vh;
}
</style>
