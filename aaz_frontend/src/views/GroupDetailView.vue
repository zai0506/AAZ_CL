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
            <v-list-item-title>我的行程</v-list-item-title>
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
          <v-tab value="transactions" prepend-icon="mdi-format-list-text">明細</v-tab>
          <v-tab value="settlement" prepend-icon="mdi-account-switch-outline">結算</v-tab>
          <v-tab value="stats" prepend-icon="mdi-chart-pie">統計</v-tab>
          <v-tab value="info" prepend-icon="mdi-information-variant-circle-outline">行程資訊</v-tab>
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
                      <template v-for="(dateGroup, index) in groupedTransactions" :key="index">
                        <!-- 日期分組標題 -->
                        <v-list-subheader class="date-group-header">
                          {{ dateGroup.date }}
                        </v-list-subheader>

                        <!-- 該日期的交易項目 -->
                        <v-list-item
                          v-for="transaction in dateGroup.transactions"
                          :key="transaction.transactionId"
                          @click="viewTransaction(transaction)"
                          class="transaction-item"
                        >
                          <!-- 圖示 -->
                          <template v-slot:prepend>
                            <v-avatar :color="getTransactionColor(transaction.type)">
                              <v-icon color="white">{{
                                getCategoryIcon(transaction)
                              }}</v-icon>
                            </v-avatar>
                          </template>

                          <!-- 內容 -->
                          <v-list-item-title class="font-weight-bold">
                            {{ transaction.title }}
                          </v-list-item-title>
                          <v-list-item-subtitle>
                            {{ transaction.category }}
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
                              <div v-if="transaction.convertedAmount && transaction.currency !== group?.baseCurrency" class="text-caption grey--text">
                                ≈ {{ formatAmount(transaction.convertedAmount, group?.baseCurrency) }}
                              </div>
                            </div>
                          </template>
                        </v-list-item>
                      </template>
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

            <!-- 行程資訊頁面 -->
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
                        <h3 class="text-h6 mb-2">行程名稱</h3>
                        <p v-if="!isGroupInfoEditing">{{ group.name }}</p>
                        <v-text-field
                          v-else
                          v-model="editingGroup.name"
                          placeholder="請輸入行程名稱"
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
              <!-- 統計期間顯示 -->
              <v-row class="mb-4">
                <v-col cols="12">
                  <div class="d-flex align-center">
                    <span class="text-subtitle-1">
                      統計期間：{{ formatStatsDateRange() }}
                    </span>
                    <v-btn
                      color="primary"
                      variant="text"
                      size="small"
                      class="ml-4"
                      @click="openStatsDateDialog"
                    >
                      設定期間
                    </v-btn>
                  </div>
                </v-col>
              </v-row>

              <v-row>
                <v-col cols="12" md="6">
                  <v-card>
                    <v-card-title>支出統計</v-card-title>
                    <v-card-text>
                      <p class="text-h5 mb-4">
                        總計: {{ formatAmount(expenseStats.total || 0, group?.baseCurrency) }}
                      </p>

                      <!-- 圓餅圖 -->
                      <div v-if="expensePieData" class="mb-6 d-flex justify-center">
                        <div style="width: 280px; height: 280px;">
                          <Pie :data="expensePieData" :options="expensePieOptions" />
                        </div>
                      </div>

                      <!-- 長條圖 -->
                      <div
                        v-for="cat in expenseStats.categoryStats || []"
                        :key="cat.category"
                        class="mb-3 category-bar"
                        :class="{ 'hovered': hoveredCategory === cat.category }"
                        @mouseenter="hoveredCategory = cat.category"
                        @mouseleave="hoveredCategory = null"
                        @click="openCategoryDetail(cat.category, 'expense')"
                        style="cursor: pointer;"
                      >
                        <div class="d-flex justify-space-between align-center">
                          <v-icon size="small" class="mr-2">{{ getCategoryIcon(cat.category) }}</v-icon>
                          <span>{{ getCategoryName(cat.category) }}</span>
                          <span>{{ cat.percentage?.toFixed(1) }}%</span>
                        </div>
                        <v-progress-linear
                          :model-value="cat.percentage || 0"
                          :color="getCategoryColor(cat.category)"
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

                      <!-- 圓餅圖 -->
                      <div v-if="incomePieData" class="mb-6 d-flex justify-center">
                        <div style="width: 280px; height: 280px;">
                          <Pie :data="incomePieData" :options="incomePieOptions" />
                        </div>
                      </div>

                      <!-- 長條圖 -->
                      <div
                        v-for="cat in incomeStats.categoryStats || []"
                        :key="cat.category"
                        class="mb-3 category-bar"
                        :class="{ 'hovered': hoveredCategory === cat.category }"
                        @mouseenter="hoveredCategory = cat.category"
                        @mouseleave="hoveredCategory = null"
                        @click="openCategoryDetail(cat.category, 'income')"
                        style="cursor: pointer;"
                      >
                        <div class="d-flex justify-space-between align-center">
                          <v-icon size="small" class="mr-2">{{ getCategoryIcon(cat.category) }}</v-icon>
                          <span>{{ getCategoryName(cat.category) }}</span>
                          <span>{{ cat.percentage?.toFixed(1) }}%</span>
                        </div>
                        <v-progress-linear
                          :model-value="cat.percentage || 0"
                          :color="getCategoryColor(cat.category)"
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

    <!-- 類別明細對話框 -->
    <v-dialog v-model="showCategoryDetailDialog" max-width="600px">
      <v-card>
        <v-card-title class="d-flex align-center bg-primary text-white">
          <span>{{ getCategoryName(selectedCategory) }} - 明細</span>
          <v-spacer></v-spacer>
          <v-btn icon="mdi-close" variant="text" size="small" color="white" @click="closeCategoryDetail"></v-btn>
        </v-card-title>

        <v-card-text class="pa-0">
          <v-list v-if="categoryTransactions.length > 0">
            <template v-for="(dateGroup, index) in groupedCategoryTransactions" :key="index">
              <!-- 日期分組標題 -->
              <v-list-subheader class="date-group-header">
                {{ dateGroup.date }}
              </v-list-subheader>

              <!-- 該日期的交易項目 -->
              <v-list-item
                v-for="transaction in dateGroup.transactions"
                :key="transaction.transactionId"
                class="transaction-item"
                @click="viewTransaction(transaction)"
              >
                <v-list-item-title>{{ transaction.title }}</v-list-item-title>
                <v-list-item-subtitle>
                  {{ transaction.category }}
                </v-list-item-subtitle>
                <template v-slot:append>
                  <div class="text-right">
                    <div class="font-weight-bold" :class="getAmountColor(transaction.type)">
                      {{ formatAmount(transaction.amount, transaction.currency) }}
                    </div>
                    <div v-if="transaction.convertedAmount && transaction.currency !== group?.baseCurrency" class="text-caption grey--text">
                      ≈ {{ formatAmount(transaction.convertedAmount, group?.baseCurrency) }}
                    </div>
                  </div>
                </template>
              </v-list-item>
            </template>
          </v-list>

          <div v-else class="text-center pa-8">
            <v-icon size="64" color="grey-lighten-1">mdi-receipt-text-outline</v-icon>
            <p class="text-body-1 grey--text mt-4">此類別暫無交易記錄</p>
          </div>
        </v-card-text>

        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="grey" variant="text" @click="closeCategoryDetail">關閉</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 統計期間設定對話框 -->
    <v-dialog v-model="showStatsDateDialog" max-width="500px">
      <v-card>
        <v-card-title class="d-flex align-center bg-primary text-white">
          <span>設定統計期間</span>
          <v-spacer></v-spacer>
          <v-btn icon="mdi-close" variant="text" size="small" color="white" @click="closeStatsDateDialog"></v-btn>
        </v-card-title>

        <v-card-text class="pt-6">
          <v-row>
            <v-col cols="12">
              <v-text-field
                v-model="tempStatsStartDate"
                label="開始日期"
                type="date"
                prepend-icon="mdi-calendar"
                density="comfortable"
                :error="!!statsDateErrorMessage"
              ></v-text-field>
            </v-col>
            <v-col cols="12">
              <v-text-field
                v-model="tempStatsEndDate"
                label="結束日期"
                type="date"
                prepend-icon="mdi-calendar"
                density="comfortable"
                :error="!!statsDateErrorMessage"
              ></v-text-field>
            </v-col>
          </v-row>

          <v-alert v-if="statsDateErrorMessage" type="error" class="mt-2">
            {{ statsDateErrorMessage }}
          </v-alert>
        </v-card-text>

        <v-card-actions>
          <v-btn variant="text" @click="clearStatsDateFilter">清除篩選</v-btn>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="closeStatsDateDialog">取消</v-btn>
          <v-btn color="primary" variant="elevated" @click="applyStatsDateFilter">
            套用
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';
import { Pie } from 'vue-chartjs';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';

import ExpenseModal from '@/components/ExpenseModal.vue';
import IncomeModal from '@/components/IncomeModal.vue';
import TransferModal from '@/components/TransferModal.vue';

// 註冊 Chart.js 組件
ChartJS.register(ArcElement, Tooltip, Legend);

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

// 類別明細對話框
const showCategoryDetailDialog = ref(false);
const selectedCategory = ref(null);
const selectedCategoryType = ref(null); // 'expense' or 'income'

// 統計期間篩選
const showStatsDateDialog = ref(false);
const statsStartDate = ref('');
const statsEndDate = ref('');
const tempStatsStartDate = ref('');
const tempStatsEndDate = ref('');
const statsDateErrorMessage = ref('');

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
    const params = {};
    if (statsStartDate.value) {
      params.startDate = statsStartDate.value;
    }
    if (statsEndDate.value) {
      params.endDate = statsEndDate.value;
    }

    const [expenseRes, incomeRes] = await Promise.all([
      axios.get(`/trips/${groupId.value}/stats/expenses`, { params }),
      axios.get(`/trips/${groupId.value}/stats/incomes`, { params }),
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

// 格式化日期為完整格式（YYYY年MM月DD日）
const formatDateFull = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}年${month}月${day}日`;
};

// 按日期分組交易（明細頁面）
const groupedTransactions = computed(() => {
  if (!transactions.value || transactions.value.length === 0) return [];

  // 按日期排序（新到舊）
  const sortedTransactions = [...transactions.value].sort((a, b) => {
    return new Date(b.transactionDate) - new Date(a.transactionDate);
  });

  // 分組
  const groups = {};
  sortedTransactions.forEach(transaction => {
    const dateKey = transaction.transactionDate.split('T')[0]; // 取得日期部分
    if (!groups[dateKey]) {
      groups[dateKey] = [];
    }
    groups[dateKey].push(transaction);
  });

  // 轉換為陣列格式
  return Object.keys(groups).map(dateKey => ({
    date: formatDateFull(dateKey),
    transactions: groups[dateKey]
  }));
});

// 按日期分組類別交易（類別明細對話框）
const groupedCategoryTransactions = computed(() => {
  if (!categoryTransactions.value || categoryTransactions.value.length === 0) return [];

  // categoryTransactions 已經在其 computed 中排序過了（新到舊）
  const sortedTransactions = categoryTransactions.value;

  // 分組
  const groups = {};
  sortedTransactions.forEach(transaction => {
    const dateKey = transaction.transactionDate.split('T')[0]; // 取得日期部分
    if (!groups[dateKey]) {
      groups[dateKey] = [];
    }
    groups[dateKey].push(transaction);
  });

  // 轉換為陣列格式
  return Object.keys(groups).map(dateKey => ({
    date: formatDateFull(dateKey),
    transactions: groups[dateKey]
  }));
});

// 格式化金額
const formatAmount = (amount, currency) => {
  if (!amount) return `${currency} 0`;
  return `${currency} ${Number(amount).toLocaleString()}`;
};

// 取得類別圖示
const getCategoryIcon = (transactionOrCategory) => {
  // 判斷傳入的是完整的 transaction 物件還是單純的 category 字串
  const category = typeof transactionOrCategory === 'object' ? transactionOrCategory.category : transactionOrCategory;
  const type = typeof transactionOrCategory === 'object' ? transactionOrCategory.type : ''; // 若只傳入字串，則類型未知，後續用預設處理

  // 轉帳類型有專屬圖示，且不需細分其他類別
  if (type === 'transfer') {
    return 'mdi-cash-sync';
  }

  const categoryIcons = {
    // 支出類別
    '美食': 'mdi-silverware-fork-knife',
    '服飾': 'mdi-tshirt-crew-outline',
    '住宿': 'mdi-bed-outline',
    '藥妝日用': 'mdi-cart-variant',
    '交通': 'mdi-plane-train',
    '景點活動': 'mdi-ski',
    '禮品': 'mdi-gift-open-outline',
    '零碎支出': 'mdi-cat',
    // 收入類別
    '退稅退費': 'mdi-cash-refund',
    '保險理賠': 'mdi-shield-check-outline',
    '贊助': 'mdi-hand-heart',
    '公積金': 'mdi-piggy-bank-outline',
    '意外之財': 'mdi-bank-plus',
  };

  // 根據類別查找圖示
  if (categoryIcons[category]) {
    return categoryIcons[category];
  } else if (type === 'income') {
    // 如果是收入但沒有找到特定的子類別圖示，給一個通用的收入圖示
    return 'mdi-cash-plus';
  } else {
    // 其他情況或未知的支出類別，給一個通用的支出圖示
    return 'mdi-cart-outline';
  }
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
    美食: '美食',
    服飾: '服飾',
    住宿: '住宿',
    藥妝日用: '藥妝日用',
    交通: '交通',
    景點活動: '景點活動',
    禮品: '禮品',
    零碎支出: '零碎支出',
  };
  return names[category] || category;
};

// 類別顏色對照表（支出）
const categoryColors = {
  美食: '#FF6384',      // 粉紅色
  服飾: '#36A2EB',      // 藍色
  住宿: '#C9CBCF',      // 黃色
  藥妝日用: '#4BC0C0',  // 青色
  交通: '#9966FF',    // 紫色
  景點活動: '#FF9F40',    // 橘色
  禮品: '#F8BBD0',    // 
  零碎支出: '#FFE066',    // 灰色
  // 收入類別
  公積金: '#A2D2FF',  // 嬰兒粉藍 (清新、透亮)
  贊助: '#8D99AE',      // 淡灰藍 (清冷色調，平衡左側暖色)
  意外之財: '#E29578',  // 陶土粉 (暖調但低飽和，像夕陽下的陶土)
  退稅退費: '#70Eaaa',  // 螢光綠 (清透自然，與左邊青色區隔)
  保險理賠: '#B8C0FF',  // 薰衣草藍 (輕柔的藍紫，舒適且不重)
  };

// 生成隨機但一致的顏色（基於類別名稱的 hash）
const getRandomColor = (category) => {
  let hash = 0;
  for (let i = 0; i < category.length; i++) {
    hash = category.charCodeAt(i) + ((hash << 5) - hash);
  }
  const hue = Math.abs(hash % 360);
  return `hsl(${hue}, 65%, 55%)`;
};

const getCategoryColor = (category) => {
  return categoryColors[category] || getRandomColor(category);
};

// 打開類別明細對話框
const openCategoryDetail = (category, type) => {
  selectedCategory.value = category;
  selectedCategoryType.value = type;
  showCategoryDetailDialog.value = true;
};

// 關閉類別明細對話框
const closeCategoryDetail = () => {
  showCategoryDetailDialog.value = false;
  selectedCategory.value = null;
  selectedCategoryType.value = null;
};

// 過濾該類別的交易（含期間篩選）
const categoryTransactions = computed(() => {
  if (!selectedCategory.value || !selectedCategoryType.value) return [];

  return transactions.value.filter(t => {
    // 類別和類型篩選
    if (t.type !== selectedCategoryType.value || t.category !== selectedCategory.value) {
      return false;
    }

    // 期間篩選
    if (statsStartDate.value || statsEndDate.value) {
      const transactionDate = new Date(t.transactionDate);
      if (statsStartDate.value) {
        const startDate = new Date(statsStartDate.value);
        if (transactionDate < startDate) return false;
      }
      if (statsEndDate.value) {
        const endDate = new Date(statsEndDate.value);
        endDate.setHours(23, 59, 59, 999); // 包含結束日當天
        if (transactionDate > endDate) return false;
      }
    }

    return true;
  }).sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));
});

// Hover 狀態
const hoveredCategory = ref(null);

// ========== 統計期間篩選函數 ==========

// 格式化統計期間顯示
const formatStatsDateRange = () => {
  if (!statsStartDate.value && !statsEndDate.value) {
    return '全期間';
  }

  const formatDateStr = (dateStr) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}/${month}/${day}`;
  };

  const start = formatDateStr(statsStartDate.value) || '開始';
  const end = formatDateStr(statsEndDate.value) || '結束';
  return `${start} ~ ${end}`;
};

// 打開統計期間設定對話框
const openStatsDateDialog = () => {
  tempStatsStartDate.value = statsStartDate.value;
  tempStatsEndDate.value = statsEndDate.value;
  statsDateErrorMessage.value = '';
  showStatsDateDialog.value = true;
};

// 關閉統計期間設定對話框
const closeStatsDateDialog = () => {
  showStatsDateDialog.value = false;
  statsDateErrorMessage.value = '';
};

// 套用統計期間篩選
const applyStatsDateFilter = async () => {
  // 驗證日期
  if (tempStatsStartDate.value && tempStatsEndDate.value) {
    if (tempStatsStartDate.value > tempStatsEndDate.value) {
      statsDateErrorMessage.value = '開始日期不得晚於結束日期';
      return;
    }
  }

  statsStartDate.value = tempStatsStartDate.value;
  statsEndDate.value = tempStatsEndDate.value;
  statsDateErrorMessage.value = '';
  showStatsDateDialog.value = false;

  // 重新載入統計資料
  await loadStats();
};

// 清除統計期間篩選
const clearStatsDateFilter = async () => {
  statsStartDate.value = '';
  statsEndDate.value = '';
  tempStatsStartDate.value = '';
  tempStatsEndDate.value = '';
  statsDateErrorMessage.value = '';
  showStatsDateDialog.value = false;

  // 重新載入統計資料
  await loadStats();
};

// 支出圓餅圖數據
const expensePieData = computed(() => {
  if (!expenseStats.value.categoryStats || expenseStats.value.categoryStats.length === 0) {
    return null;
  }

  const categories = expenseStats.value.categoryStats;
  return {
    labels: categories.map(cat => getCategoryName(cat.category)),
    datasets: [{
      data: categories.map(cat => cat.amount),
      backgroundColor: categories.map(cat => getCategoryColor(cat.category)),
      borderWidth: 2,
      borderColor: '#ffffff',
    }]
  };
});

// 收入圓餅圖數據
const incomePieData = computed(() => {
  if (!incomeStats.value.categoryStats || incomeStats.value.categoryStats.length === 0) {
    return null;
  }

  const categories = incomeStats.value.categoryStats;
  return {
    labels: categories.map(cat => getCategoryName(cat.category)),
    datasets: [{
      data: categories.map(cat => cat.amount),
      backgroundColor: categories.map(cat => getCategoryColor(cat.category)),
      borderWidth: 2,
      borderColor: '#ffffff',
    }]
  };
});

// 支出圓餅圖配置
const expensePieOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: true,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: function(context) {
          const label = context.label || '';
          const value = context.parsed || 0;
          const total = context.dataset.data.reduce((a, b) => a + b, 0);
          const percentage = ((value / total) * 100).toFixed(1);
          return `${label}: ${percentage}%`;
        }
      }
    }
  },
  onHover: (event, activeElements) => {
    if (activeElements.length > 0) {
      const index = activeElements[0].index;
      const category = expenseStats.value.categoryStats?.[index]?.category;
      hoveredCategory.value = category;
    } else {
      hoveredCategory.value = null;
    }
  }
}));

// 收入圓餅圖配置
const incomePieOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: true,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: function(context) {
          const label = context.label || '';
          const value = context.parsed || 0;
          const total = context.dataset.data.reduce((a, b) => a + b, 0);
          const percentage = ((value / total) * 100).toFixed(1);
          return `${label}: ${percentage}%`;
        }
      }
    }
  },
  onHover: (event, activeElements) => {
    if (activeElements.length > 0) {
      const index = activeElements[0].index;
      const category = incomeStats.value.categoryStats?.[index]?.category;
      hoveredCategory.value = category;
    } else {
      hoveredCategory.value = null;
    }
  }
}));

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

.date-group-header {
  background-color: #f5f5f5;
  font-weight: bold;
  color: #666;
  padding: 12px 16px;
  font-size: 0.875rem;
  border-top: 1px solid #e0e0e0;
  border-bottom: 1px solid #e0e0e0;
  margin-top: 0 !important;
}

.date-group-header:first-child {
  border-top: none;
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

/* 統計頁面長條圖 hover 效果 */
.category-bar {
  transition: all 0.3s ease;
  padding: 8px;
  margin: 0 -8px;
  border-radius: 8px;
}

.category-bar.hovered {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.category-bar:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
</style>
