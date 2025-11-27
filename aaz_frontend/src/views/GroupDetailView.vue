<template>
  <v-app>
    <!-- 頂部導航列 -->
    <v-app-bar color="primary" dark elevation="4">
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

    <v-main>
      <!-- 功能標籤頁 -->
      <v-tabs v-model="currentTab" bg-color="white" color="primary" class="elevation-2">
        <v-tab value="transactions">明細</v-tab>
        <v-tab value="info">群組資訊</v-tab>
        <v-tab value="settlement">結算</v-tab>
        <v-tab value="stats">統計</v-tab>
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
                          <v-icon color="white">{{ getTransactionIcon(transaction.type) }}</v-icon>
                        </v-avatar>
                      </template>

                      <!-- 內容 -->
                      <v-list-item-title class="font-weight-bold">
                        {{ transaction.title }}
                      </v-list-item-title>
                      <v-list-item-subtitle>
                        {{ transaction.category }} · {{ formatDate(transaction.transactionDate) }}
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
                  <p class="text-body-2 grey--text">點擊右下角的「+」按鈕新增第一筆交易</p>
                </v-card>
              </v-col>
            </v-row>
          </v-window-item>

          <!-- 群組資訊頁面 -->
          <v-window-item value="info">
            <v-card v-if="group">
              <v-card-text>
                <v-row>
                  <v-col cols="12">
                    <h3 class="text-h6 mb-2">群組名稱</h3>
                    <p>{{ group.name }}</p>
                  </v-col>

                  <v-col cols="12">
                    <h3 class="text-h6 mb-2">旅遊日期</h3>
                    <p>{{ formatDate(group.startDate) }} - {{ formatDate(group.endDate) }}</p>
                  </v-col>

                  <v-col cols="12">
                    <h3 class="text-h6 mb-2">主要貨幣</h3>
                    <p>{{ group.baseCurrency }}</p>
                  </v-col>

                  <v-col cols="12">
                    <h3 class="text-h6 mb-2">公告欄</h3>
                    <p>{{ group.announcement || '無' }}</p>
                  </v-col>

                  <v-col cols="12">
                    <h3 class="text-h6 mb-2">成員列表</h3>
                    <v-chip
                      v-for="member in group.members"
                      :key="member.id"
                      class="ma-1"
                      :color="member.isCreator ? 'primary' : 'default'"
                    >
                      <v-icon v-if="member.isCreator" start>mdi-crown</v-icon>
                      {{ member.displayName }}
                    </v-chip>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-window-item>

          <!-- 結算頁面 -->
          <v-window-item value="settlement">
            <v-card v-if="balanceReport">
              <!-- 餘額長條圖 -->
              <v-card-title>成員餘額</v-card-title>
              <v-card-text>
                <div v-for="balance in balanceReport.balances" :key="balance.memberId" class="mb-4">
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
    </v-main>

    <!-- ========== 浮動新增按鈕========== -->
    <v-btn
      icon="mdi-plus"
      color="primary"
      size="x-large"
      style="position: fixed; bottom: 100px; left: 50%; transform: translateX(-50%); z-index: 999"
      elevation="8"
      @click="showAddDialog = true"
    ></v-btn>

    <!-- 新增交易對話框（三個頁籤）-->
    <v-dialog v-model="showAddDialog" max-width="650px" persistent scrollable>
      <v-card>
        <v-card-title class="d-flex justify-space-between align-center bg-grey-lighten-4">
          <span class="text-h6">新增交易</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="closeAddDialog"></v-btn>
        </v-card-title>

        <!-- 三個頁籤 -->
        <v-tabs v-model="addTab" bg-color="white" color="primary" grow>
          <v-tab value="expense">
            <v-icon left color="red">mdi-cash-minus</v-icon>
            支出
          </v-tab>
          <v-tab value="income">
            <v-icon left color="green">mdi-cash-plus</v-icon>
            收入
          </v-tab>
          <v-tab value="transfer">
            <v-icon left color="blue">mdi-swap-horizontal</v-icon>
            轉帳
          </v-tab>
        </v-tabs>

        <v-divider></v-divider>

        <!-- 頁籤內容 -->
        <v-window v-model="addTab">
          <!-- ==================== 支出頁籤 ==================== -->
          <v-window-item value="expense">
            <v-card-text style="max-height: 500px; overflow-y: auto">
              <v-form ref="expenseForm" v-model="expenseValid">
                <!-- 日期 -->
                <v-text-field
                  v-model="expenseData.expenseDate"
                  label="日期"
                  type="date"
                  prepend-icon="mdi-calendar"
                  density="comfortable"
                  required
                ></v-text-field>

                <!-- 品項 -->
                <v-text-field
                  v-model="expenseData.title"
                  label="品項"
                  prepend-icon="mdi-format-title"
                  placeholder="例如：午餐、飯店住宿"
                  density="comfortable"
                  required
                ></v-text-field>

                <!-- 類別 -->
                <v-select
                  v-model="expenseData.category"
                  :items="['食', '衣', '住', '日用品', '交通', '娛樂', '其他']"
                  label="類別"
                  prepend-icon="mdi-shape"
                  density="comfortable"
                  required
                ></v-select>

                <!-- 貨幣和金額 -->
                <v-row>
                  <v-col cols="4">
                    <v-select
                      v-model="expenseData.currency"
                      :items="currencies"
                      label="貨幣"
                      density="comfortable"
                      required
                    ></v-select>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      v-model="expenseData.amount"
                      label="金額"
                      type="number"
                      prepend-icon="mdi-currency-usd"
                      density="comfortable"
                      required
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 匯率 -->
                <v-text-field
                  v-model="expenseData.exchangeRate"
                  label="匯率（選填）"
                  type="number"
                  step="0.0001"
                  prepend-icon="mdi-swap-horizontal"
                  hint="例如：1 USD = 30 TWD，則填 30"
                  density="comfortable"
                ></v-text-field>

                <!-- 誰先付錢 -->
                <v-card variant="outlined" class="mb-4 mt-4">
                  <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
                    <v-icon left size="small">mdi-wallet</v-icon>
                    誰先付錢
                  </v-card-title>
                  <v-card-text>
                    <v-list density="compact">
                      <v-list-item v-for="member in group?.members" :key="member.id">
                        <template v-slot:prepend>
                          <v-checkbox
                            v-model="expenseData.selectedPayers"
                            :value="member.id"
                            hide-details
                            density="compact"
                          ></v-checkbox>
                        </template>
                        <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                        <template v-slot:append>
                          <v-text-field
                            v-if="expenseData.selectedPayers.includes(member.id)"
                            v-model="expenseData.paymentAmounts[member.id]"
                            type="number"
                            density="compact"
                            hide-details
                            style="width: 100px"
                          ></v-text-field>
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-card-text>
                </v-card>

                <!-- 如何分攤 -->
                <v-card variant="outlined" class="mb-4">
                  <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
                    <v-icon left size="small">mdi-account-multiple</v-icon>
                    如何分攤
                  </v-card-title>
                  <v-card-text>
                    <v-radio-group v-model="expenseData.splitType" hide-details class="mb-2">
                      <v-radio label="平均分攤" value="equal"></v-radio>
                      <v-radio label="自訂金額" value="custom"></v-radio>
                    </v-radio-group>

                    <v-list density="compact">
                      <v-list-item v-for="member in group?.members" :key="member.id">
                        <template v-slot:prepend>
                          <v-checkbox
                            v-model="expenseData.selectedSplitters"
                            :value="member.id"
                            hide-details
                            density="compact"
                          ></v-checkbox>
                        </template>
                        <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                        <template v-slot:append>
                          <v-text-field
                            v-if="
                              expenseData.selectedSplitters.includes(member.id) &&
                              expenseData.splitType === 'custom'
                            "
                            v-model="expenseData.splitAmounts[member.id]"
                            type="number"
                            density="compact"
                            hide-details
                            style="width: 100px"
                          ></v-text-field>
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-card-text>
                </v-card>

                <!-- 備註 -->
                <v-textarea
                  v-model="expenseData.notes"
                  label="備註（選填）"
                  prepend-icon="mdi-note-text"
                  rows="2"
                  density="comfortable"
                ></v-textarea>
              </v-form>
            </v-card-text>

            <v-divider></v-divider>

            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn variant="text" @click="closeAddDialog">取消</v-btn>
              <v-btn color="red" variant="elevated" @click="submitExpense">新增支出</v-btn>
            </v-card-actions>
          </v-window-item>

          <!-- ==================== 收入頁籤 ==================== -->
          <v-window-item value="income">
            <v-card-text style="max-height: 500px; overflow-y: auto">
              <v-form ref="incomeForm" v-model="incomeValid">
                <!-- 日期 -->
                <v-text-field
                  v-model="incomeData.incomeDate"
                  label="日期"
                  type="date"
                  prepend-icon="mdi-calendar"
                  density="comfortable"
                  required
                ></v-text-field>

                <!-- 品項 -->
                <v-text-field
                  v-model="incomeData.title"
                  label="品項"
                  prepend-icon="mdi-format-title"
                  placeholder="例如：景點退費、保險理賠"
                  density="comfortable"
                  required
                ></v-text-field>

                <!-- 類別 -->
                <v-select
                  v-model="incomeData.category"
                  :items="['退費', '保險', '禮金', '獎金', '其他']"
                  label="類別"
                  prepend-icon="mdi-shape"
                  density="comfortable"
                  required
                ></v-select>

                <!-- 貨幣和金額 -->
                <v-row>
                  <v-col cols="4">
                    <v-select
                      v-model="incomeData.currency"
                      :items="currencies"
                      label="貨幣"
                      density="comfortable"
                      required
                    ></v-select>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      v-model="incomeData.amount"
                      label="金額"
                      type="number"
                      prepend-icon="mdi-currency-usd"
                      density="comfortable"
                      required
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 匯率 -->
                <v-text-field
                  v-model="incomeData.exchangeRate"
                  label="匯率（選填）"
                  type="number"
                  step="0.0001"
                  prepend-icon="mdi-swap-horizontal"
                  density="comfortable"
                ></v-text-field>

                <!-- 誰先收款 -->
                <v-card variant="outlined" class="mb-4 mt-4">
                  <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
                    <v-icon left size="small">mdi-wallet</v-icon>
                    誰先收款
                  </v-card-title>
                  <v-card-text>
                    <v-list density="compact">
                      <v-list-item v-for="member in group?.members" :key="member.id">
                        <template v-slot:prepend>
                          <v-checkbox
                            v-model="incomeData.selectedReceivers"
                            :value="member.id"
                            hide-details
                            density="compact"
                          ></v-checkbox>
                        </template>
                        <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                        <template v-slot:append>
                          <v-text-field
                            v-if="incomeData.selectedReceivers.includes(member.id)"
                            v-model="incomeData.receiveAmounts[member.id]"
                            type="number"
                            density="compact"
                            hide-details
                            style="width: 100px"
                          ></v-text-field>
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-card-text>
                </v-card>

                <!-- 如何分攤 -->
                <v-card variant="outlined" class="mb-4">
                  <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
                    <v-icon left size="small">mdi-account-multiple</v-icon>
                    如何分攤
                  </v-card-title>
                  <v-card-text>
                    <v-radio-group v-model="incomeData.splitType" hide-details class="mb-2">
                      <v-radio label="平均分攤" value="equal"></v-radio>
                      <v-radio label="自訂金額" value="custom"></v-radio>
                    </v-radio-group>

                    <v-list density="compact">
                      <v-list-item v-for="member in group?.members" :key="member.id">
                        <template v-slot:prepend>
                          <v-checkbox
                            v-model="incomeData.selectedSplitters"
                            :value="member.id"
                            hide-details
                            density="compact"
                          ></v-checkbox>
                        </template>
                        <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                        <template v-slot:append>
                          <v-text-field
                            v-if="
                              incomeData.selectedSplitters.includes(member.id) &&
                              incomeData.splitType === 'custom'
                            "
                            v-model="incomeData.splitAmounts[member.id]"
                            type="number"
                            density="compact"
                            hide-details
                            style="width: 100px"
                          ></v-text-field>
                        </template>
                      </v-list-item>
                    </v-list>
                  </v-card-text>
                </v-card>

                <!-- 備註 -->
                <v-textarea
                  v-model="incomeData.notes"
                  label="備註（選填）"
                  prepend-icon="mdi-note-text"
                  rows="2"
                  density="comfortable"
                ></v-textarea>
              </v-form>
            </v-card-text>

            <v-divider></v-divider>

            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn variant="text" @click="closeAddDialog">取消</v-btn>
              <v-btn color="green" variant="elevated" @click="submitIncome">新增收入</v-btn>
            </v-card-actions>
          </v-window-item>

          <!-- ==================== 轉帳頁籤 ==================== -->
          <v-window-item value="transfer">
            <v-card-text style="max-height: 500px; overflow-y: auto">
              <v-form ref="transferForm" v-model="transferValid">
                <!-- 日期 -->
                <v-text-field
                  v-model="transferData.transferDate"
                  label="日期"
                  type="date"
                  prepend-icon="mdi-calendar"
                  density="comfortable"
                  required
                ></v-text-field>

                <!-- 誰轉帳 -->
                <v-select
                  v-model="transferData.fromMemberId"
                  :items="group?.members"
                  item-title="displayName"
                  item-value="id"
                  label="誰轉帳"
                  prepend-icon="mdi-account-arrow-right"
                  density="comfortable"
                  required
                ></v-select>

                <!-- 轉帳給誰 -->
                <v-select
                  v-model="transferData.toMemberId"
                  :items="group?.members"
                  item-title="displayName"
                  item-value="id"
                  label="轉帳給誰"
                  prepend-icon="mdi-account-arrow-left"
                  density="comfortable"
                  required
                ></v-select>

                <!-- 貨幣和金額 -->
                <v-row>
                  <v-col cols="4">
                    <v-select
                      v-model="transferData.currency"
                      :items="currencies"
                      label="貨幣"
                      density="comfortable"
                      required
                    ></v-select>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      v-model="transferData.amount"
                      label="金額"
                      type="number"
                      prepend-icon="mdi-currency-usd"
                      density="comfortable"
                      required
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 匯率 -->
                <v-text-field
                  v-model="transferData.exchangeRate"
                  label="匯率（選填）"
                  type="number"
                  step="0.0001"
                  prepend-icon="mdi-swap-horizontal"
                  density="comfortable"
                ></v-text-field>

                <!-- 備註 -->
                <v-textarea
                  v-model="transferData.notes"
                  label="備註（選填）"
                  prepend-icon="mdi-note-text"
                  placeholder="例如：還午餐錢、飯店房費"
                  rows="2"
                  density="comfortable"
                ></v-textarea>
              </v-form>
            </v-card-text>

            <v-divider></v-divider>

            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn variant="text" @click="closeAddDialog">取消</v-btn>
              <v-btn color="blue" variant="elevated" @click="submitTransfer">新增轉帳</v-btn>
            </v-card-actions>
          </v-window-item>
        </v-window>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';

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

// ========== 新增 Modal 狀態 ==========
const showAddDialog = ref(false);
const addTab = ref('expense'); // 預設顯示支出

// 貨幣選項
const currencies = ['TWD', 'JPY', 'USD', 'EUR', 'CNY', 'KRW', 'THB', 'SGD', 'HKD'];

// 表單驗證
const expenseForm = ref(null);
const incomeForm = ref(null);
const transferForm = ref(null);
const expenseValid = ref(false);
const incomeValid = ref(false);
const transferValid = ref(false);

// 支出表單資料
const expenseData = ref({
  expenseDate: new Date().toISOString().split('T')[0],
  title: '',
  category: '食',
  amount: '',
  currency: 'TWD',
  exchangeRate: '',
  notes: '',
  selectedPayers: [],
  paymentAmounts: {},
  selectedSplitters: [],
  splitAmounts: {},
  splitType: 'equal',
});

// 收入表單資料
const incomeData = ref({
  incomeDate: new Date().toISOString().split('T')[0],
  title: '',
  category: '退費',
  amount: '',
  currency: 'TWD',
  exchangeRate: '',
  notes: '',
  selectedReceivers: [],
  receiveAmounts: {},
  selectedSplitters: [],
  splitAmounts: {},
  splitType: 'equal',
});

// 轉帳表單資料
const transferData = ref({
  transferDate: new Date().toISOString().split('T')[0],
  fromMemberId: null,
  toMemberId: null,
  amount: '',
  currency: 'TWD',
  exchangeRate: '',
  notes: '',
});

// 關閉對話框
const closeAddDialog = () => {
  showAddDialog.value = false;
  // 重置表單（可選）
};

// 提交支出
const submitExpense = async () => {
  console.log('提交支出:', expenseData.value);
  // TODO: 呼叫 API
  closeAddDialog();
  await refreshAllData();
};

// 提交收入
const submitIncome = async () => {
  console.log('提交收入:', incomeData.value);
  // TODO: 呼叫 API
  closeAddDialog();
  await refreshAllData();
};

// 提交轉帳
const submitTransfer = async () => {
  console.log('提交轉帳:', transferData.value);
  // TODO: 呼叫 API
  closeAddDialog();
  await refreshAllData();
};

// 取得群組 ID（使用 computed 保持一致）
const groupId = computed(() => parseInt(route.params.id));

// 計算最大餘額（用於長條圖）
const maxBalance = computed(() => {
  if (!balanceReport.value?.balances) return 0;
  return Math.max(...balanceReport.value.balances.map((b) => Math.abs(b.balance)));
});

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

// ========== 新增：重新載入所有資料（Modal 新增後呼叫）==========
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

// 查看交易詳情
const viewTransaction = (transaction) => {
  console.log('查看交易:', transaction);
  // TODO: 開啟交易詳情對話框
};

// 登出
const handleLogout = () => {
  userStore.logout();
  router.push('/login');
};

// 頁面載入時執行
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
</style>
