<template>
  <v-app>
    <!-- 頂部導航列 -->
    <v-app-bar elevation="8" class="custom-navbar">
      <v-btn icon @click="router.push('/home')">
        <v-icon>mdi-arrow-left</v-icon>
      </v-btn>

      <v-toolbar-title class="font-weight-bold">
        {{ group?.name || '載入中...' }}
      </v-toolbar-title>

      <v-spacer></v-spacer>

      <!-- 使用者選單 -->
      <v-menu>
        <template v-slot:activator="{ props }">
          <v-btn icon v-bind="props">
            <v-avatar color="white" size="36">
              <span class="primary--text font-weight-bold">
                {{ userStore.nickname?.charAt(0) || 'U' }}
              </span>
            </v-avatar>
          </v-btn>
        </template>

        <v-list>
          <v-list-item>
            <v-list-item-title class="font-weight-bold">
              {{ userStore.nickname }}
            </v-list-item-title>
          </v-list-item>
          <v-divider></v-divider>
          <v-list-item @click="router.push('/home')">
            <template v-slot:prepend>
              <v-icon>mdi-home</v-icon>
            </template>
            <v-list-item-title>我的群組</v-list-item-title>
          </v-list-item>
          <v-list-item @click="handleLogout">
            <template v-slot:prepend>
              <v-icon>mdi-logout</v-icon>
            </template>
            <v-list-item-title>登出</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>

    <v-main class="page-background">
      <!-- 白色內容區塊（包含 tabs 和內容）-->
      <div class="content-wrapper-with-tabs">
        <!-- 功能標籤頁 -->
        <v-tabs v-model="currentTab" bg-color="transparent" color="primary" grow>
          <v-tab value="transactions">明細</v-tab>
          <v-tab value="settlement">結算</v-tab>
          <v-tab value="stats">統計</v-tab>
          <v-tab value="info">群組資訊</v-tab>
        </v-tabs>

        <v-container fluid class="pa-6">
          <v-window v-model="currentTab">
            <!-- 交易明細頁面 -->
            <v-window-item value="transactions">
              <v-row>
                <v-col cols="12">
                  <!-- 交易列表 -->
                  <v-card v-if="transactions.length > 0">
                    <v-list>
                      <v-list-item
                        v-for="transaction in transactions"
                        :key="transaction.transactionId"
                        @click="viewTransaction(transaction)"
                        class="transaction-item"
                      >
                        <!-- 圖示 -->
                        <template v-slot:prepend>
                          <v-avatar :color="getTransactionColor(transaction.type)">
                            <v-icon color="white">{{
                              getTransactionIcon(transaction.type)
                            }}</v-icon>
                          </v-avatar>
                        </template>

                        <!-- 內容 -->
                        <v-list-item-title class="font-weight-bold">
                          {{ transaction.title }}
                        </v-list-item-title>
                        <v-list-item-subtitle>
                          {{ transaction.category }} · {{ formatDate(transaction.transactionDate) }}
                          <span
                            v-if="
                              transaction.currency !== group?.baseCurrency &&
                              transaction.exchangeRate
                            "
                          >
                            · 匯率: {{ transaction.exchangeRate }}
                          </span>
                        </v-list-item-subtitle>

                        <!-- 金額 -->
                        <template v-slot:append>
                          <div class="text-right">
                            <div class="font-weight-bold" :class="getAmountColor(transaction.type)">
                              {{ formatAmount(transaction.amount, transaction.currency) }}
                            </div>
                            <div v-if="transaction.convertedAmount" class="text-caption grey--text">
                              ≈ {{ formatAmount(transaction.convertedAmount, group?.baseCurrency) }}
                            </div>
                          </div>
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-card>

                  <!-- 空狀態 -->
                  <v-card v-else class="text-center pa-12">
                    <v-icon size="96" color="grey-lighten-1">mdi-receipt-text-outline</v-icon>
                    <p class="text-h6 grey--text mt-4">還沒有交易記錄</p>
                    <p class="text-body-2 grey--text">點擊下方「+」按鈕新增第一筆交易</p>
                  </v-card>
                </v-col>
              </v-row>
            </v-window-item>

            <!-- 群組資訊頁面 -->
            <v-window-item value="info">
              <v-card v-if="group">
                <v-card-title class="d-flex align-center">
                  <v-spacer></v-spacer>
                  <template v-if="!isGroupInfoEditing">
                    <v-btn icon="mdi-pencil" variant="text" @click="startGroupInfoEdit"></v-btn>
                  </template>
                  <template v-else>
                    <v-btn variant="text" @click="cancelGroupInfoEdit">取消</v-btn>
                    <v-btn
                      color="primary"
                      variant="elevated"
                      @click="saveGroupInfoChanges"
                      :disabled="!formValid || !!dateErrorMessage"
                      >儲存</v-btn
                    >
                  </template>
                </v-card-title>
                <v-card-text>
                  <v-form ref="groupInfoForm" v-model="formValid">
                    <v-row>
                      <v-col cols="12">
                        <h3 class="text-h6 mb-2">群組名稱</h3>
                        <p v-if="!isGroupInfoEditing">{{ group.name }}</p>
                        <v-text-field
                          v-else
                          v-model="editingGroup.name"
                          placeholder="請輸入群組名稱"
                          hide-details
                        ></v-text-field>
                      </v-col>

                      <v-col cols="12">
                        <div class="d-flex align-center mb-2">
                          <h3 class="text-h6">旅遊日期</h3>
                          <span v-if="dateErrorMessage" class="text-red text-caption ml-2">{{
                            dateErrorMessage
                          }}</span>
                        </div>
                        <p v-if="!isGroupInfoEditing">
                          {{ formatDate(group.startDate) }} - {{ formatDate(group.endDate) }}
                        </p>
                        <div v-else class="d-flex align-center">
                          <v-text-field
                            v-model="editingGroup.startDate"
                            type="date"
                            density="compact"
                            hide-details
                            :error="!!dateErrorMessage"
                          ></v-text-field>
                          <span class="mx-2">-</span>
                          <v-text-field
                            v-model="editingGroup.endDate"
                            type="date"
                            density="compact"
                            hide-details
                            :error="!!dateErrorMessage"
                          ></v-text-field>
                        </div>
                      </v-col>

                      <v-col cols="12">
                        <h3 class="text-h6 mb-2">主要貨幣</h3>
                        <p v-if="!isGroupInfoEditing">{{ group.baseCurrency }}</p>
                        <v-select
                          v-else
                          v-model="editingGroup.baseCurrency"
                          :items="currencies"
                          density="compact"
                          hide-details
                          readonly
                        ></v-select>
                      </v-col>

                      <v-col cols="12">
                        <div class="d-flex align-center justify-space-between mb-2">
                          <h3 class="text-h6">成員列表</h3>
                          <!-- 編輯模式時，新增成員按鈕放在右邊 -->
                          <v-btn
                            v-if="isGroupInfoEditing"
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

                        <!-- 非編輯模式：垂直列表顯示 -->
                        <v-list v-if="!isGroupInfoEditing" density="compact">
                          <v-list-item
                            v-for="member in sortedMembers"
                            :key="member.id"
                            :prepend-icon="member.isCreator ? 'mdi-crown' : 'mdi-account'"
                          >
                            <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                            <template v-slot:append v-if="member.isCreator">
                              <v-chip color="primary" size="small">建立者</v-chip>
                            </template>
                          </v-list-item>
                        </v-list>

                        <!-- 編輯模式：可編輯的垂直列表 -->
                        <v-list v-else density="compact">
                          <v-list-item
                            v-for="member in sortedMembers"
                            :key="member.id"
                            :prepend-icon="member.isCreator ? 'mdi-crown' : 'mdi-account'"
                          >
                            <!-- Creator 不可編輯 -->
                            <v-list-item-title v-if="member.isCreator">
                              {{ member.displayName }}
                            </v-list-item-title>

                            <!-- 正在編輯的成員：顯示輸入框 -->
                            <v-text-field
                              v-else-if="editingMembers[member.id]?.isEditing"
                              v-model="editingMembers[member.id].newName"
                              @blur="finishEditMember(member.id)"
                              @keyup.enter="finishEditMember(member.id)"
                              density="compact"
                              hide-details
                              autofocus
                            ></v-text-field>

                            <!-- 未編輯或編輯完成的成員：可點擊 -->
                            <v-list-item-title
                              v-else
                              class="member-name-editable"
                              @click="startEditMember(member)"
                            >
                              {{ editingMembers[member.id]?.newName || member.displayName }}
                            </v-list-item-title>

                            <template v-slot:append>
                              <!-- Creator 標籤 -->
                              <v-chip v-if="member.isCreator" color="primary" size="small"
                                >建立者</v-chip
                              >
                              <!-- 已編輯標記 -->
                              <v-icon
                                v-else-if="
                                  editingMembers[member.id] && !editingMembers[member.id].isEditing
                                "
                                color="success"
                                >mdi-check-circle</v-icon
                              >
                              <!-- 可編輯提示 -->
                              <v-icon
                                v-else-if="!editingMembers[member.id]?.isEditing"
                                size="small"
                                color="grey-lighten-1"
                                >mdi-pencil</v-icon
                              >
                            </template>
                          </v-list-item>

                          <!-- 動態新增的成員輸入框 -->
                          <v-list-item
                            v-for="(member, index) in newMembers"
                            :key="member.tempId"
                            prepend-icon="mdi-account-plus"
                          >
                            <v-text-field
                              v-model="member.name"
                              placeholder="輸入成員名稱"
                              density="compact"
                              hide-details
                              @blur="handleNewMemberBlur(member)"
                              :ref="
                                (el) => {
                                  if (index === newMembers.length - 1) latestMemberInput = el;
                                }
                              "
                            ></v-text-field>
                          </v-list-item>
                        </v-list>
                      </v-col>

                      <v-col cols="12">
                        <h3 class="text-h6 mb-2">公告欄</h3>
                        <p v-if="!isGroupInfoEditing">{{ group.announcement || '無' }}</p>
                        <v-textarea
                          v-else
                          v-model="editingGroup.announcement"
                          placeholder="請輸入公告內容"
                          rows="3"
                          density="compact"
                          hide-details
                        ></v-textarea>
                      </v-col>
                    </v-row>
                  </v-form>
                </v-card-text>
              </v-card>
            </v-window-item>

            <!-- 結算頁面 -->
            <v-window-item value="settlement">
              <!-- 空狀態 -->
              <div v-if="balanceReport && nonZeroBalances.length === 0" class="text-center py-16">
                <v-icon size="120" color="grey-lighten-2">mdi-check-circle-outline</v-icon>
                <p class="text-h5 mt-4 text-grey">目前無結算項目</p>
                <p class="text-body-1 text-grey">所有成員已結清，沒有需要轉帳的項目</p>
              </div>

              <!-- 有餘額時顯示卡片 -->
              <v-card v-else-if="balanceReport && nonZeroBalances.length > 0">
                <!-- 餘額長條圖 -->
                <v-card-title>成員餘額</v-card-title>
                <v-card-text>
                  <div
                    v-for="balance in nonZeroBalances"
                    :key="balance.memberId"
                    class="mb-4"
                  >
                    <div class="d-flex justify-space-between mb-1">
                      <span>{{ balance.memberName }}</span>
                      <span :class="balance.balance >= 0 ? 'text-green' : 'text-red'">
                        {{ formatAmount(Math.abs(balance.balance), group?.baseCurrency) }}
                      </span>
                    </div>
                    <v-progress-linear
                      :model-value="Math.abs(balance.balance)"
                      :max="maxBalance"
                      :color="balance.balance >= 0 ? 'green' : 'red'"
                      height="20"
                    ></v-progress-linear>
                  </div>
                </v-card-text>

                <v-divider></v-divider>

                <!-- 結算建議 -->
                <v-card-title>結算方案（最少轉帳次數）</v-card-title>
                <v-card-text>
                  <v-list v-if="balanceReport.debts && balanceReport.debts.length > 0">
                    <v-list-item v-for="(debt, index) in balanceReport.debts" :key="index">
                      <v-list-item-title>
                        <v-icon color="primary">mdi-arrow-right-bold</v-icon>
                        <strong>{{ debt.fromMemberName }}</strong> 付給
                        <strong>{{ debt.toMemberName }}</strong>
                      </v-list-item-title>
                      <template v-slot:append>
                        <v-chip color="primary">
                          {{ formatAmount(debt.amount, group?.baseCurrency) }}
                        </v-chip>
                      </template>
                    </v-list-item>
                  </v-list>
                  <p v-else class="text-center grey--text py-4">目前沒有需要結算的項目</p>
                </v-card-text>
              </v-card>
            </v-window-item>

            <!-- 統計頁面 -->
            <v-window-item value="stats">
              <v-row>
                <v-col cols="12" md="6">
                  <v-card>
                    <v-card-title>支出統計</v-card-title>
                    <v-card-text>
                      <p class="text-h5 mb-4">
                        總計: {{ formatAmount(expenseStats.total || 0, group?.baseCurrency) }}
                      </p>
                      <div
                        v-for="cat in expenseStats.categoryStats || []"
                        :key="cat.category"
                        class="mb-3"
                      >
                        <div class="d-flex justify-space-between">
                          <span>{{ getCategoryName(cat.category) }}</span>
                          <span>{{ cat.percentage?.toFixed(1) }}%</span>
                        </div>
                        <v-progress-linear
                          :model-value="cat.percentage || 0"
                          color="primary"
                          height="8"
                        ></v-progress-linear>
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>

                <v-col cols="12" md="6">
                  <v-card>
                    <v-card-title>收入統計</v-card-title>
                    <v-card-text>
                      <p class="text-h5 mb-4">
                        總計: {{ formatAmount(incomeStats.total || 0, group?.baseCurrency) }}
                      </p>
                      <div
                        v-for="cat in incomeStats.categoryStats || []"
                        :key="cat.category"
                        class="mb-3"
                      >
                        <div class="d-flex justify-space-between">
                          <span>{{ getCategoryName(cat.category) }}</span>
                          <span>{{ cat.percentage?.toFixed(1) }}%</span>
                        </div>
                        <v-progress-linear
                          :model-value="cat.percentage || 0"
                          color="success"
                          height="8"
                        ></v-progress-linear>
                      </div>
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>
            </v-window-item>
          </v-window>
        </v-container>
      </div>
    </v-main>

    <!-- ========== 浮動新增按鈕 (選單) ========== -->
    <v-menu location="top center">
      <template v-slot:activator="{ props }">
        <v-btn
          v-if="currentTab === 'transactions'"
          icon="mdi-plus"
          color="primary"
          size="x-large"
          style="position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 999"
          elevation="8"
          v-bind="props"
        ></v-btn>
      </template>
      <v-list>
        <v-list-item
          @click="openAddModal('expense')"
          prepend-icon="mdi-cart-outline"
          title="新增支出"
        ></v-list-item>
        <v-list-item
          @click="openAddModal('income')"
          prepend-icon="mdi-cash-plus"
          title="新增收入"
        ></v-list-item>
        <v-list-item
          @click="openAddModal('transfer')"
          prepend-icon="mdi-bank-transfer"
          title="新增轉帳"
        ></v-list-item>
      </v-list>
    </v-menu>

    <!-- ========== 交易 Modal ========== -->
    <!-- ========== 交易 Modals ========== -->
    <ExpenseModal
      v-model="showExpenseModal"
      :trip-id="groupId"
      :members="group?.members"
      :base-currency="group?.baseCurrency"
      :transaction="selectedTransaction"
      @refresh="refreshAllData"
    />

    <IncomeModal
      v-model="showIncomeModal"
      :trip-id="groupId"
      :members="group?.members"
      :base-currency="group?.baseCurrency"
      :transaction="selectedTransaction"
      @refresh="refreshAllData"
    />

    <TransferModal
      v-model="showTransferModal"
      :trip-id="groupId"
      :members="group?.members"
      :base-currency="group?.baseCurrency"
      :transaction="selectedTransaction"
      @refresh="refreshAllData"
    />
  </v-app>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';

import ExpenseModal from '@/components/ExpenseModal.vue';
import IncomeModal from '@/components/IncomeModal.vue';
import TransferModal from '@/components/TransferModal.vue';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 狀態
const currentTab = ref('transactions');
const group = ref(null);
const transactions = ref([]);
const balanceReport = ref(null);
const expenseStats = ref({});
const incomeStats = ref({});

// Modal 狀態
const showExpenseModal = ref(false);
const showIncomeModal = ref(false);
const showTransferModal = ref(false);
const selectedTransaction = ref(null);

// ========== 群組資訊編輯狀態 ==========
const isGroupInfoEditing = ref(false);
const editingGroup = ref({});
const newMembers = ref([]);
const latestMemberInput = ref(null);
const editingMembers = ref({});
const groupInfoForm = ref(null);
const formValid = ref(true);

// 貨幣選項
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

// 取得群組 ID
const groupId = computed(() => parseInt(route.params.id));

// 開啟新增 Modal
const openAddModal = (type) => {
  selectedTransaction.value = null; // 新增模式
  if (type === 'expense') showExpenseModal.value = true;
  if (type === 'income') showIncomeModal.value = true;
  if (type === 'transfer') showTransferModal.value = true;
};

// 開啟查看/編輯 Modal
const viewTransaction = (transaction) => {
  selectedTransaction.value = transaction; // 查看模式
  if (transaction.type === 'expense') showExpenseModal.value = true;
  if (transaction.type === 'income') showIncomeModal.value = true;
  if (transaction.type === 'transfer') showTransferModal.value = true;
};

// 載入群組資訊
const loadGroup = async () => {
  try {
    const response = await axios.get(`/trips/${groupId.value}`);
    group.value = response.data;
  } catch (error) {
    console.error('載入群組失敗:', error);
  }
};

// 載入交易列表
const loadTransactions = async () => {
  try {
    const response = await axios.get(`/trips/${groupId.value}/transactions`);
    transactions.value = response.data;
  } catch (error) {
    console.error('載入交易失敗:', error);
  }
};

// 載入結算資訊
const loadBalanceReport = async () => {
  try {
    const response = await axios.get(`/trips/${groupId.value}/settlement`);
    balanceReport.value = response.data;
  } catch (error) {
    console.error('載入結算失敗:', error);
  }
};

// 載入統計資訊
const loadStats = async () => {
  try {
    const [expenseRes, incomeRes] = await Promise.all([
      axios.get(`/trips/${groupId.value}/stats/expenses`),
      axios.get(`/trips/${groupId.value}/stats/incomes`),
    ]);
    expenseStats.value = expenseRes.data;
    incomeStats.value = incomeRes.data;
  } catch (error) {
    console.error('載入統計失敗:', error);
  }
};

// 重新載入所有資料
const refreshAllData = async () => {
  await loadTransactions();
  await loadBalanceReport();
  await loadStats();
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleDateString('zh-TW');
};

// 格式化金額
const formatAmount = (amount, currency) => {
  if (!amount) return `${currency} 0`;
  return `${currency} ${Number(amount).toLocaleString()}`;
};

// 取得交易類型圖示
const getTransactionIcon = (type) => {
  const icons = {
    expense: 'mdi-cart-outline',
    income: 'mdi-cash-plus',
    transfer: 'mdi-bank-transfer',
  };
  return icons[type] || 'mdi-help';
};

// 取得交易類型顏色
const getTransactionColor = (type) => {
  const colors = {
    expense: 'red',
    income: 'green',
    transfer: 'blue',
  };
  return colors[type] || 'grey';
};

// 取得金額顏色
const getAmountColor = (type) => {
  return type === 'expense' ? 'text-red' : type === 'income' ? 'text-green' : 'text-blue';
};

// 取得類別名稱
const getCategoryName = (category) => {
  const names = {
    食: '飲食',
    衣: '服飾',
    住: '住宿',
    日用品: '日用品',
    交通: '交通',
    娛樂: '娛樂',
    其他: '其他',
  };
  return names[category] || category;
};

// 登出
const handleLogout = () => {
  userStore.logout();
  router.push('/login');
};

// ========== 群組資訊編輯邏輯 (保留) ==========

const isAddMemberDisabled = computed(() => {
  if (newMembers.value.length === 0) return false;
  const lastMember = newMembers.value[newMembers.value.length - 1];
  return lastMember.name.trim() === '';
});

const dateErrorMessage = computed(() => {
  if (
    editingGroup.value.startDate &&
    editingGroup.value.endDate &&
    editingGroup.value.startDate > editingGroup.value.endDate
  ) {
    return '起始日不得晚於結束日';
  }
  return '';
});

const handleNewMemberBlur = (member) => {
  if (member.name.trim() === '') {
    newMembers.value = newMembers.value.filter((m) => m.tempId !== member.tempId);
  }
};

const addNewMemberField = async () => {
  newMembers.value.push({ tempId: Date.now(), name: '' });
  await nextTick();
  if (latestMemberInput.value) {
    latestMemberInput.value.focus();
  }
};

const startGroupInfoEdit = () => {
  editingGroup.value = JSON.parse(JSON.stringify(group.value));
  if (group.value.startDate) {
    editingGroup.value.startDate = group.value.startDate.split('T')[0];
  }
  if (group.value.endDate) {
    editingGroup.value.endDate = group.value.endDate.split('T')[0];
  }
  newMembers.value = [];
  editingMembers.value = {};
  isGroupInfoEditing.value = true;
};

const cancelGroupInfoEdit = () => {
  isGroupInfoEditing.value = false;
  newMembers.value = [];
  editingMembers.value = {};
};

const startEditMember = (member) => {
  editingMembers.value[member.id] = {
    isEditing: true,
    newName: member.displayName,
  };
};

const finishEditMember = (memberId) => {
  const editState = editingMembers.value[memberId];
  if (editState && editState.newName.trim() !== '') {
    editState.isEditing = false;
  } else {
    delete editingMembers.value[memberId];
  }
};

const saveGroupInfoChanges = async () => {
  try {
    const membersToSend = [];
    Object.keys(editingMembers.value).forEach((memberId) => {
      const editState = editingMembers.value[memberId];
      if (editState.newName && editState.newName.trim() !== '') {
        membersToSend.push({
          id: parseInt(memberId),
          displayName: editState.newName.trim(),
        });
      }
    });

    const actualNewMembers = newMembers.value
      .map((m) => ({ displayName: m.name.trim() }))
      .filter((m) => m.displayName !== '');
    membersToSend.push(...actualNewMembers);

    const updateRequest = {
      name: editingGroup.value.name,
      startDate: editingGroup.value.startDate,
      endDate: editingGroup.value.endDate,
      baseCurrency: editingGroup.value.baseCurrency,
      announcement: editingGroup.value.announcement,
      members: membersToSend.length > 0 ? membersToSend : null,
    };

    await axios.put(`/trips/${groupId.value}`, updateRequest);
    await loadGroup();
    isGroupInfoEditing.value = false;
    newMembers.value = [];
    editingMembers.value = {};
  } catch (error) {
    console.error('更新群組資訊失敗:', error);
    alert('更新失敗：' + (error.response?.data?.message || error.message));
  }
};

const sortedMembers = computed(() => {
  if (!group.value?.members) return [];
  return [...group.value.members].sort((a, b) => {
    if (a.isCreator) return -1;
    if (b.isCreator) return 1;
    return 0;
  });
});

// 過濾掉餘額為 0 的成員
const nonZeroBalances = computed(() => {
  if (!balanceReport.value?.balances) return [];
  return balanceReport.value.balances.filter((b) => Math.abs(b.balance) > 0.01);
});

const maxBalance = computed(() => {
  if (!nonZeroBalances.value || nonZeroBalances.value.length === 0) return 0;
  return Math.max(...nonZeroBalances.value.map((b) => Math.abs(b.balance)));
});

onMounted(async () => {
  userStore.loadUserFromStorage();
  await loadGroup();
  await loadTransactions();
  await loadBalanceReport();
  await loadStats();
});
</script>

<style scoped>
.transaction-item {
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
}

.transaction-item:hover {
  background-color: #f5f5f5;
}

.text-red {
  color: #f44336;
}

.text-green {
  color: #4caf50;
}

.text-blue {
  color: #2196f3;
}

/* GroupDetailView 專用：包含 tabs 的容器 */
.content-wrapper-with-tabs {
  position: relative;
  z-index: 1;
  max-width: 60% !important;
  margin: 0 auto;
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
}

/* tabs 整體樣式 */
.content-wrapper-with-tabs .v-tabs {
  flex-shrink: 0;
}

/* 每個 tab 的底線 */
.content-wrapper-with-tabs :deep(.v-tab) {
  position: relative;
}

.content-wrapper-with-tabs :deep(.v-tab::after) {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 2px;
  background-color: rgba(0, 0, 0, 0.12);
}

.content-wrapper-with-tabs :deep(.v-tab--selected::after) {
  background-color: rgb(var(--v-theme-primary));
}

.content-wrapper-with-tabs .v-container {
  flex: 1;
}

/* 自訂導覽列樣式 */
.custom-navbar {
  background-color: rgba(245, 245, 220, 0.9) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important;
  max-width: 60% !important;
  margin: 0 auto !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  right: auto !important;
  position: fixed !important;
  top: 0 !important;
}

.custom-navbar :deep(.v-toolbar-title),
.custom-navbar :deep(.v-btn),
.custom-navbar :deep(.v-icon) {
  color: #333 !important;
}

.custom-navbar :deep(.v-avatar) {
  color: #333 !important;
}

@media (max-width: 768px) {
  .content-wrapper-with-tabs {
    max-width: 60% !important;
  }
}

:deep(.v-app-bar) {
  max-width: 60% !important;
}

.member-name-editable {
  cursor: pointer;
  transition: all 0.2s;
}

.member-name-editable:hover {
  color: rgb(var(--v-theme-primary));
}
</style>
