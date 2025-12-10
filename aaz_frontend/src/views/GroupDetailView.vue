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
                  <!-- 「群組資訊」文字已移除 -->
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
              <v-card v-if="balanceReport">
                <!-- 餘額長條圖 -->
                <v-card-title>成員餘額</v-card-title>
                <v-card-text>
                  <div
                    v-for="balance in balanceReport.balances"
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

    <!-- ========== 浮動新增按鈕========== -->
    <v-btn
      v-if="currentTab === 'transactions'"
      icon="mdi-plus"
      color="primary"
      size="x-large"
      style="position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 999"
      elevation="8"
      @click="showAddDialog = true"
    ></v-btn>

    <!-- 交易詳情對話框 -->
    <v-dialog v-model="showDetailDialog" max-width="650px" persistent scrollable>
      <v-card v-if="selectedTransaction">
        <v-card-title class="d-flex justify-space-between align-center bg-grey-lighten-4">
          <span class="text-h6">交易詳情</span>
          <v-btn icon="mdi-close" variant="text" size="small" @click="closeDetailDialog"></v-btn>
        </v-card-title>

        <v-divider></v-divider>

        <v-card-text style="max-height: 500px; overflow-y: auto" @click="showTypeWarning = false">
          <!-- ==================== 支出詳情 ==================== -->
          <v-form v-if="editingTransaction.type === 'expense'" @click.stop>
            <!-- 交易類型 -->
            <v-text-field
              :model-value="getTransactionTypeName(editingTransaction.type)"
              label="交易類型"
              prepend-icon="mdi-swap-horizontal"
              readonly
              density="comfortable"
              @click="isEditing && (showTypeWarning = true)"
              :hide-details="!showTypeWarning || !isEditing"
              :class="{ 'mb-3': !isEditing || !showTypeWarning }"
            ></v-text-field>
            <div
              v-if="showTypeWarning && isEditing"
              class="text-red text-caption mb-3 ml-10"
              style="margin-top: -8px"
            >
              交易類型不得修改，若欲更改請刪除後新增
            </div>

            <!-- 日期 -->
            <v-text-field
              v-model="editingTransaction.transactionDate"
              label="日期"
              type="date"
              prepend-icon="mdi-calendar"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-text-field>

            <!-- 品項 -->
            <v-text-field
              v-model="editingTransaction.title"
              label="品項"
              prepend-icon="mdi-format-title"
              placeholder="例如：午餐、飯店住宿"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-text-field>

            <!-- 類別 -->
            <v-select
              v-model="editingTransaction.category"
              :items="['食', '衣', '住', '日用品', '交通', '娛樂', '其他']"
              label="類別"
              prepend-icon="mdi-shape"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-select>

            <!-- 貨幣和金額 -->
            <v-row>
              <v-col cols="4">
                <v-select
                  v-model="editingTransaction.currency"
                  :items="currencies"
                  label="貨幣"
                  prepend-icon="mdi-currency-usd"
                  :readonly="!isEditing"
                  density="comfortable"
                  required
                ></v-select>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="editingTransaction.amount"
                  label="金額"
                  type="number"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  :readonly="!isEditing"
                  density="comfortable"
                  required
                ></v-text-field>
              </v-col>
            </v-row>

            <!-- 匯率和換算後金額 -->
            <v-row v-if="showEditExchangeRate">
              <v-col cols="4">
                <v-text-field
                  v-model="editingTransaction.exchangeRate"
                  label="匯率"
                  type="number"
                  step="0.0001"
                  :hint="`1 ${editingTransaction.currency} / ${group.baseCurrency}`"
                  :readonly="!isEditing"
                  density="comfortable"
                  :rules="isEditing ? [rules.required, rules.positive] : []"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  :model-value="editingConvertedAmount"
                  label="換算後金額"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  :readonly="!isEditing"
                  density="comfortable"
                ></v-text-field>
              </v-col>
            </v-row>

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
                        v-model="editingTransaction.selectedPayers"
                        :value="member.id"
                        :readonly="!isEditing"
                        hide-details
                        density="compact"
                      ></v-checkbox>
                    </template>
                    <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                    <template v-slot:append>
                      <v-text-field
                        v-if="editingTransaction.selectedPayers?.includes(member.id)"
                        v-model="editingTransaction.paymentAmounts[member.id]"
                        type="number"
                        :readonly="!isEditing"
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
                <v-radio-group
                  v-model="editingTransaction.splitType"
                  :readonly="!isEditing"
                  hide-details
                  class="mb-2"
                >
                  <v-radio label="平均分攤" value="equal"></v-radio>
                  <v-radio label="自訂金額" value="custom"></v-radio>
                </v-radio-group>

                <v-list density="compact">
                  <v-list-item v-for="member in group?.members" :key="member.id">
                    <template v-slot:prepend>
                      <v-checkbox
                        v-model="editingTransaction.selectedSplitters"
                        :value="member.id"
                        :readonly="!isEditing"
                        hide-details
                        density="compact"
                      ></v-checkbox>
                    </template>
                    <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                    <template v-slot:append>
                      <v-text-field
                        v-if="
                          editingTransaction.selectedSplitters?.includes(member.id) &&
                          editingTransaction.splitType === 'custom'
                        "
                        v-model="editingTransaction.splitAmounts[member.id]"
                        type="number"
                        :readonly="!isEditing"
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
              v-model="editingTransaction.notes"
              label="備註（選填）"
              prepend-icon="mdi-note-text"
              rows="2"
              :readonly="!isEditing"
              density="comfortable"
            ></v-textarea>
          </v-form>

          <!-- ==================== 收入詳情 ==================== -->
          <v-form v-else-if="editingTransaction.type === 'income'" @click.stop>
            <!-- 交易類型 -->
            <v-text-field
              :model-value="getTransactionTypeName(editingTransaction.type)"
              label="交易類型"
              prepend-icon="mdi-swap-horizontal"
              readonly
              density="comfortable"
              @click="isEditing && (showTypeWarning = true)"
              :hide-details="!showTypeWarning || !isEditing"
              :class="{ 'mb-3': !isEditing || !showTypeWarning }"
            ></v-text-field>
            <div
              v-if="showTypeWarning && isEditing"
              class="text-red text-caption mb-3 ml-10"
              style="margin-top: -8px"
            >
              交易類型不得修改，若欲更改請刪除後新增
            </div>

            <!-- 日期 -->
            <v-text-field
              v-model="editingTransaction.transactionDate"
              label="日期"
              type="date"
              prepend-icon="mdi-calendar"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-text-field>

            <!-- 品項 -->
            <v-text-field
              v-model="editingTransaction.title"
              label="品項"
              prepend-icon="mdi-format-title"
              placeholder="例如：景點退費、保險理賠"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-text-field>

            <!-- 類別 -->
            <v-select
              v-model="editingTransaction.category"
              :items="['退費', '保險', '禮金', '獎金', '其他']"
              label="類別"
              prepend-icon="mdi-shape"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-select>

            <!-- 貨幣和金額 -->
            <v-row>
              <v-col cols="4">
                <v-select
                  v-model="editingTransaction.currency"
                  :items="currencies"
                  label="貨幣"
                  prepend-icon="mdi-currency-usd"
                  :readonly="!isEditing"
                  density="comfortable"
                  required
                ></v-select>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="editingTransaction.amount"
                  label="金額"
                  type="number"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  :readonly="!isEditing"
                  density="comfortable"
                  required
                ></v-text-field>
              </v-col>
            </v-row>

            <!-- 匯率和換算後金額 -->
            <v-row v-if="showEditExchangeRate">
              <v-col cols="4">
                <v-text-field
                  v-model="editingTransaction.exchangeRate"
                  label="匯率"
                  type="number"
                  step="0.0001"
                  :hint="`1 ${editingTransaction.currency} / ${group.baseCurrency}`"
                  :readonly="!isEditing"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  density="comfortable"
                  :rules="isEditing ? [rules.required, rules.positive] : []"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  :model-value="editingConvertedAmount"
                  label="換算後金額"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  :readonly="!isEditing"
                  density="comfortable"
                ></v-text-field>
              </v-col>
            </v-row>

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
                        v-model="editingTransaction.selectedReceivers"
                        :value="member.id"
                        :readonly="!isEditing"
                        hide-details
                        density="compact"
                      ></v-checkbox>
                    </template>
                    <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                    <template v-slot:append>
                      <v-text-field
                        v-if="editingTransaction.selectedReceivers?.includes(member.id)"
                        v-model="editingTransaction.receiveAmounts[member.id]"
                        type="number"
                        :readonly="!isEditing"
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
                <v-radio-group
                  v-model="editingTransaction.splitType"
                  :readonly="!isEditing"
                  hide-details
                  class="mb-2"
                >
                  <v-radio label="平均分攤" value="equal"></v-radio>
                  <v-radio label="自訂金額" value="custom"></v-radio>
                </v-radio-group>

                <v-list density="compact">
                  <v-list-item v-for="member in group?.members" :key="member.id">
                    <template v-slot:prepend>
                      <v-checkbox
                        v-model="editingTransaction.selectedSplitters"
                        :value="member.id"
                        :readonly="!isEditing"
                        hide-details
                        density="compact"
                      ></v-checkbox>
                    </template>
                    <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                    <template v-slot:append>
                      <v-text-field
                        v-if="
                          editingTransaction.selectedSplitters?.includes(member.id) &&
                          editingTransaction.splitType === 'custom'
                        "
                        v-model="editingTransaction.splitAmounts[member.id]"
                        type="number"
                        :readonly="!isEditing"
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
              v-model="editingTransaction.notes"
              label="備註（選填）"
              prepend-icon="mdi-note-text"
              rows="2"
              :readonly="!isEditing"
              density="comfortable"
            ></v-textarea>
          </v-form>

          <!-- ==================== 轉帳詳情 ==================== -->
          <v-form v-else-if="editingTransaction.type === 'transfer'" @click.stop>
            <!-- 交易類型 -->
            <v-text-field
              :model-value="getTransactionTypeName(editingTransaction.type)"
              label="交易類型"
              prepend-icon="mdi-swap-horizontal"
              readonly
              density="comfortable"
              @click="isEditing && (showTypeWarning = true)"
              :hide-details="!showTypeWarning || !isEditing"
              :class="{ 'mb-3': !isEditing || !showTypeWarning }"
            ></v-text-field>
            <div
              v-if="showTypeWarning && isEditing"
              class="text-red text-caption mb-3 ml-10"
              style="margin-top: -8px"
            >
              交易類型不得修改，若欲更改請刪除後新增
            </div>

            <!-- 日期 -->
            <v-text-field
              v-model="editingTransaction.transactionDate"
              label="日期"
              type="date"
              prepend-icon="mdi-calendar"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-text-field>

            <!-- 誰轉帳 -->
            <v-select
              v-model="editingTransaction.fromMemberId"
              :items="group?.members"
              item-title="displayName"
              item-value="id"
              label="誰轉帳"
              prepend-icon="mdi-account-arrow-right"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-select>

            <!-- 轉帳給誰 -->
            <v-select
              v-model="editingTransaction.toMemberId"
              :items="group?.members"
              item-title="displayName"
              item-value="id"
              label="轉帳給誰"
              prepend-icon="mdi-account-arrow-left"
              :readonly="!isEditing"
              density="comfortable"
              required
            ></v-select>

            <!-- 貨幣和金額 -->
            <v-row>
              <v-col cols="4">
                <v-select
                  v-model="editingTransaction.currency"
                  :items="currencies"
                  label="貨幣"
                  :readonly="!isEditing"
                  density="comfortable"
                  required
                  prepend-icon="mdi-currency-usd"
                ></v-select>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="editingTransaction.amount"
                  label="金額"
                  type="number"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  :readonly="!isEditing"
                  density="comfortable"
                  required
                ></v-text-field>
              </v-col>
            </v-row>

            <!-- 匯率和換算後金額 -->
            <v-row v-if="showEditExchangeRate">
              <v-col cols="4">
                <v-text-field
                  v-model="editingTransaction.exchangeRate"
                  label="匯率"
                  type="number"
                  step="0.0001"
                  :hint="`1 ${editingTransaction.currency} / ${group.baseCurrency}`"
                  :readonly="!isEditing"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  density="comfortable"
                  :rules="isEditing ? [rules.required, rules.positive] : []"
                  required
                ></v-text-field>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  :model-value="editingConvertedAmount"
                  label="換算後金額"
                  prepend-icon="mdi-circle-small"
                  class="invisible-icon"
                  :readonly="!isEditing"
                  density="comfortable"
                ></v-text-field>
              </v-col>
            </v-row>

            <!-- 備註 -->
            <v-textarea
              v-model="editingTransaction.notes"
              label="備註（選填）"
              prepend-icon="mdi-note-text"
              rows="2"
              :readonly="!isEditing"
              density="comfortable"
            ></v-textarea>
          </v-form>
        </v-card-text>

        <v-divider></v-divider>

        <v-card-actions>
          <v-spacer></v-spacer>
          <template v-if="!isEditing">
            <v-btn color="primary" variant="elevated" @click="startEditing">
              <v-icon start>mdi-pencil</v-icon>
              編輯
            </v-btn>
          </template>
          <template v-else>
            <v-btn variant="text" @click="cancelEditing">取消</v-btn>
            <v-btn color="primary" variant="elevated" @click="saveTransaction">
              <v-icon start>mdi-content-save</v-icon>
              儲存
            </v-btn>
          </template>
        </v-card-actions>
      </v-card>
    </v-dialog>

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
                      prepend-icon="mdi-currency-usd"
                      required
                    ></v-select>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      v-model="expenseData.amount"
                      label="金額"
                      type="number"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      density="comfortable"
                      required
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 匯率和換算後金額 -->
                <v-row v-if="showExpenseExchangeRate">
                  <v-col cols="4">
                    <v-text-field
                      v-model="expenseData.exchangeRate"
                      label="匯率"
                      type="number"
                      step="0.0001"
                      :hint="`1 ${expenseData.currency} / ${group.baseCurrency}`"
                      density="comfortable"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      :rules="[rules.required, rules.positive]"
                      required
                    ></v-text-field>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      :model-value="expenseConvertedAmount"
                      label="換算後金額"
                      :readonly="!isEditing"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      density="comfortable"
                    ></v-text-field>
                  </v-col>
                </v-row>

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
                      prepend-icon="mdi-currency-usd"
                      density="comfortable"
                      required
                    ></v-select>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      v-model="incomeData.amount"
                      label="金額"
                      type="number"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      density="comfortable"
                      required
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 匯率和換算後金額 -->
                <v-row v-if="showIncomeExchangeRate">
                  <v-col cols="4">
                    <v-text-field
                      v-model="incomeData.exchangeRate"
                      label="匯率"
                      type="number"
                      step="0.0001"
                      :hint="`1 ${incomeData.currency} / ${group.baseCurrency}`"
                      density="comfortable"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      :rules="[rules.required, rules.positive]"
                      required
                    ></v-text-field>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      :model-value="incomeConvertedAmount"
                      label="換算後金額"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      :readonly="!isEditing"
                      density="comfortable"
                    ></v-text-field>
                  </v-col>
                </v-row>

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
                      prepend-icon="mdi-currency-usd"
                      required
                    ></v-select>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      v-model="transferData.amount"
                      label="金額"
                      type="number"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      density="comfortable"
                      required
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 匯率和換算後金額 -->
                <v-row v-if="showTransferExchangeRate">
                  <v-col cols="4">
                    <v-text-field
                      v-model="transferData.exchangeRate"
                      label="匯率"
                      type="number"
                      step="0.0001"
                      :hint="`1 ${transferData.currency} / ${group.baseCurrency}`"
                      density="comfortable"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      :rules="[rules.required, rules.positive]"
                      required
                    ></v-text-field>
                  </v-col>
                  <v-col cols="8">
                    <v-text-field
                      :model-value="transferConvertedAmount"
                      label="換算後金額"
                      prepend-icon="mdi-circle-small"
                      class="invisible-icon"
                      :readonly="!isEditing"
                      density="comfortable"
                    ></v-text-field>
                  </v-col>
                </v-row>

                <!-- 備註 -->
                <v-textarea
                  v-model="transferData.notes"
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
              <v-btn color="blue" variant="elevated" @click="submitTransfer">新增轉帳</v-btn>
            </v-card-actions>
          </v-window-item>
        </v-window>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import axios from '@/api/axios';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 狀態
const currentTab = ref('transactions');
const group = ref(null);
const transactions = ref([
  {
    transactionId: 1,
    title: '午餐 - 鼎泰豐',
    category: '食',
    transactionDate: '2024-01-15',
    type: 'expense',
    amount: 1200,
    currency: 'JPY',
    convertedAmount: 1200,
    exchangeRate: '1',
  },
  {
    transactionId: 2,
    title: '景點門票退費',
    category: '退費',
    transactionDate: '2024-01-16',
    type: 'income',
    amount: 50,
    currency: 'USD',
    convertedAmount: 1500,
    exchangeRate: '30',
  },
  {
    transactionId: 3,
    title: '還飯店費用',
    category: '轉帳',
    transactionDate: '2024-01-17',
    type: 'transfer',
    amount: 3000,
    currency: 'TWD',
    convertedAmount: 3000,
    exchangeRate: '1',
  },
]); // 假資料
const balanceReport = ref(null);
const expenseStats = ref({});
const incomeStats = ref({});

// ========== 新增 Modal 狀態 ==========
const showAddDialog = ref(false);
const addTab = ref('expense'); // 預設顯示支出

// ========== 交易詳情 Modal 狀態 ==========
const showDetailDialog = ref(false);
const selectedTransaction = ref(null);
const isEditing = ref(false);
const editingTransaction = ref({});
const showTypeWarning = ref(false); // 交易類型警示狀態

// ========== 群組資訊編輯狀態 ==========
const isGroupInfoEditing = ref(false);
const editingGroup = ref({});
const newMembers = ref([]); // 用於動態新增的成員欄位
const latestMemberInput = ref(null); // 用於存儲最新輸入框的引用
const editingMembers = ref({}); // 追蹤哪些成員正在被編輯 { memberId: { isEditing: true, newName: "..." } }

const groupInfoForm = ref(null); // v-form 的 ref
const formValid = ref(true); // 表單驗證狀態

// 判斷「新增成員」按鈕是否應禁用
const isAddMemberDisabled = computed(() => {
  if (newMembers.value.length === 0) {
    return false; // 如果沒有新成員欄位，則不禁用
  }
  const lastMember = newMembers.value[newMembers.value.length - 1];
  return lastMember.name.trim() === ''; // 如果最後一個欄位是空的，則禁用
});

// 產生日期區間的錯誤訊息
const dateErrorMessage = computed(() => {
  if (
    editingGroup.value.startDate &&
    editingGroup.value.endDate &&
    editingGroup.value.startDate > editingGroup.value.endDate
  ) {
    return '起始日不得晚於結束日';
  }
  return ''; // 返回空字串表示沒有錯誤
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

const startGroupInfoEdit = () => {
  // 深拷貝一份原始資料，以便取消時還原
  editingGroup.value = JSON.parse(JSON.stringify(group.value));
  // 日期需要特殊處理，因為 API 回傳的可能是完整時間字串
  if (group.value.startDate) {
    editingGroup.value.startDate = group.value.startDate.split('T')[0];
  }
  if (group.value.endDate) {
    editingGroup.value.endDate = group.value.endDate.split('T')[0];
  }
  newMembers.value = []; // 開始編輯時，清空新成員列表
  editingMembers.value = {}; // 清空編輯中的成員狀態
  isGroupInfoEditing.value = true;
};

const cancelGroupInfoEdit = () => {
  isGroupInfoEditing.value = false;
  newMembers.value = []; // 取消編輯時，也清空新成員列表
  editingMembers.value = {}; // 清空編輯中的成員狀態
};

// 開始編輯某個成員
const startEditMember = (member) => {
  editingMembers.value[member.id] = {
    isEditing: true,
    newName: member.displayName,
  };
};

// 完成編輯某個成員
const finishEditMember = (memberId) => {
  const editState = editingMembers.value[memberId];
  if (editState && editState.newName.trim() !== '') {
    // 保留編輯狀態，但標記為不再編輯中
    editState.isEditing = false;
  } else {
    // 如果名稱為空，恢復原始名稱
    delete editingMembers.value[memberId];
  }
};

const saveGroupInfoChanges = async () => {
  try {
    // 準備成員資料
    const membersToSend = [];

    // 1. 處理已編輯的現有成員（有 id，表示要更新）
    Object.keys(editingMembers.value).forEach((memberId) => {
      const editState = editingMembers.value[memberId];
      if (editState.newName && editState.newName.trim() !== '') {
        membersToSend.push({
          id: parseInt(memberId),
          displayName: editState.newName.trim(),
        });
      }
    });

    // 2. 處理新增的成員（無 id，表示新增）
    const actualNewMembers = newMembers.value
      .map((m) => ({ displayName: m.name.trim() }))
      .filter((m) => m.displayName !== '');
    membersToSend.push(...actualNewMembers);

    // 準備更新請求
    const updateRequest = {
      name: editingGroup.value.name,
      startDate: editingGroup.value.startDate,
      endDate: editingGroup.value.endDate,
      baseCurrency: editingGroup.value.baseCurrency,
      announcement: editingGroup.value.announcement,
      members: membersToSend.length > 0 ? membersToSend : null,
    };

    console.log('送出更新請求:', updateRequest);

    // 呼叫後端 API
    await axios.put(`/trips/${groupId.value}`, updateRequest);

    // 重新載入群組資料
    await loadGroup();

    // 關閉編輯模式並清空狀態
    isGroupInfoEditing.value = false;
    newMembers.value = [];
    editingMembers.value = {};
  } catch (error) {
    console.error('更新群組資訊失敗:', error);
    alert('更新失敗：' + (error.response?.data?.message || error.message));
  }
};

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

// 判斷是否需要顯示匯率欄位（支出）
const showExpenseExchangeRate = computed(() => {
  return expenseData.value.currency !== group.value?.baseCurrency;
});

// 判斷是否需要顯示匯率欄位（收入）
const showIncomeExchangeRate = computed(() => {
  return incomeData.value.currency !== group.value?.baseCurrency;
});

// 判斷是否需要顯示匯率欄位（轉帳）
const showTransferExchangeRate = computed(() => {
  return transferData.value.currency !== group.value?.baseCurrency;
});

// 計算換算後金額 (支出)
const expenseConvertedAmount = computed(() => {
  if (expenseData.value.amount && expenseData.value.exchangeRate) {
    const result =
      parseFloat(expenseData.value.amount) * parseFloat(expenseData.value.exchangeRate);
    return result.toFixed(2);
  }
  return '';
});

// 計算換算後金額 (收入)
const incomeConvertedAmount = computed(() => {
  if (incomeData.value.amount && incomeData.value.exchangeRate) {
    const result = parseFloat(incomeData.value.amount) * parseFloat(incomeData.value.exchangeRate);
    return result.toFixed(2);
  }
  return '';
});

// 計算換算後金額 (轉帳)
const transferConvertedAmount = computed(() => {
  if (transferData.value.amount && transferData.value.exchangeRate) {
    const result =
      parseFloat(transferData.value.amount) * parseFloat(transferData.value.exchangeRate);
    return result.toFixed(2);
  }
  return '';
});

// 判斷是否需要顯示匯率欄位（編輯）
const showEditExchangeRate = computed(() => {
  return editingTransaction.value.currency !== group.value?.baseCurrency;
});

// 成員列表排序：creator 排在首位
const sortedMembers = computed(() => {
  if (!group.value?.members) return [];
  return [...group.value.members].sort((a, b) => {
    if (a.isCreator) return -1;
    if (b.isCreator) return 1;
    return 0;
  });
});

// 監聽支出貨幣變化，自動設定匯率
watch(
  () => expenseData.value.currency,
  (newCurrency) => {
    if (newCurrency === group.value?.baseCurrency) {
      expenseData.value.exchangeRate = '1';
    } else {
      expenseData.value.exchangeRate = '';
    }
  }
);

// 監聽收入貨幣變化，自動設定匯率
watch(
  () => incomeData.value.currency,
  (newCurrency) => {
    if (newCurrency === group.value?.baseCurrency) {
      incomeData.value.exchangeRate = '1';
    } else {
      incomeData.value.exchangeRate = '';
    }
  }
);

// 監聽轉帳貨幣變化，自動設定匯率
watch(
  () => transferData.value.currency,
  (newCurrency) => {
    if (newCurrency === group.value?.baseCurrency) {
      transferData.value.exchangeRate = '1';
    } else {
      transferData.value.exchangeRate = '';
    }
  }
);

// 監聽編輯中的貨幣變化，自動設定匯率
watch(
  () => editingTransaction.value.currency,
  (newCurrency) => {
    if (newCurrency === group.value?.baseCurrency) {
      editingTransaction.value.exchangeRate = '1';
    } else {
      if (!editingTransaction.value.exchangeRate || editingTransaction.value.exchangeRate === '1') {
        editingTransaction.value.exchangeRate = '';
      }
    }
  }
);

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

// 取得交易類型中文名稱
const getTransactionTypeName = (type) => {
  const names = {
    expense: '支出',
    income: '收入',
    transfer: '轉帳',
  };
  return names[type] || type;
};

// 查看交易詳情
const viewTransaction = (transaction) => {
  selectedTransaction.value = transaction;
  editingTransaction.value = { ...transaction }; // 複製一份用於編輯
  isEditing.value = false;
  showDetailDialog.value = true;
};

// 開始編輯
const startEditing = () => {
  isEditing.value = true;
};

// 儲存編輯
const saveTransaction = () => {
  console.log('儲存交易:', editingTransaction.value);
  // TODO: 呼叫 API 更新交易
  // 暫時更新本地資料
  const index = transactions.value.findIndex(
    (t) => t.transactionId === editingTransaction.value.transactionId
  );
  if (index !== -1) {
    transactions.value[index] = { ...editingTransaction.value };
    selectedTransaction.value = { ...editingTransaction.value };
  }
  isEditing.value = false;
};

// 取消編輯
const cancelEditing = () => {
  editingTransaction.value = { ...selectedTransaction.value }; // 恢復原始資料
  isEditing.value = false;
  showTypeWarning.value = false; // 重置警示狀態
};

// 關閉詳情視窗
const closeDetailDialog = () => {
  showDetailDialog.value = false;
  isEditing.value = false;
  selectedTransaction.value = null;
  showTypeWarning.value = false; // 重置警示狀態
};

// 登出
const handleLogout = () => {
  userStore.logout();
  router.push('/login');
};

const rules = {
  required: (v) => !!v || '此欄位必填',
  positive: (v) => v > 0 || '金額必須大於 0',
};

const editingConvertedAmount = computed(() => {
  if (editingTransaction.value.amount && editingTransaction.value.exchangeRate) {
    const result =
      parseFloat(editingTransaction.value.amount) *
      parseFloat(editingTransaction.value.exchangeRate);
    return result.toFixed(2);
  }
  return '';
});

// 頁面載入時執行
onMounted(async () => {
  userStore.loadUserFromStorage();
  await loadGroup();
  // await loadTransactions();  // 暫時註解以顯示假資料
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

/* 隱藏貨幣欄位的圖示 */
.invisible-icon :deep(.v-input__prepend) {
  opacity: 0;
}

/* GroupDetailView 專用：包含 tabs 的容器 */
.content-wrapper-with-tabs {
  position: relative;
  z-index: 1;
  max-width: 60% !important;
  margin: 0 auto;
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  min-height: calc(100vh - 64px); /* 減去 app-bar 高度 */
  display: flex;
  flex-direction: column;
  /* 沒有 border-radius = 沒有圓角 */
}

/* tabs 整體樣式 */
.content-wrapper-with-tabs .v-tabs {
  flex-shrink: 0; /* \\防止 tabs 被壓縮 */
}

/* 每個 tab 的底線 - 佔 60% 寬度，彼此不連接 */
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

/* 選中的 tab 底線變成主色 */
.content-wrapper-with-tabs :deep(.v-tab--selected::after) {
  background-color: rgb(var(--v-theme-primary));
}

/* 確保內容區域可以撐開 */
.content-wrapper-with-tabs .v-container {
  flex: 1;
}

/* 自訂導覽列樣式 */
.custom-navbar {
  background-color: rgba(245, 245, 220, 0.9) !important; /* 米白色 + 90% 不透明度 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3) !important; /* 加深陰影 */
  max-width: 60% !important;
  margin: 0 auto !important; /* ← 關鍵：自動置中 */
  left: 50% !important; /* ← 從螢幕中心開始 */
  transform: translateX(-50%) !important; /* ← 往左移自己寬度的 50% */
  right: auto !important; /* ← 取消 right 定位 */
  position: fixed !important;
  top: 0 !important;
}

/* 導覽列內的文字和按鈕改為深色 */
.custom-navbar :deep(.v-toolbar-title),
.custom-navbar :deep(.v-btn),
.custom-navbar :deep(.v-icon) {
  color: #333 !important;
}

/* 頭像內的文字也改為深色 */
.custom-navbar :deep(.v-avatar) {
  color: #333 !important;
}

@media (max-width: 768px) {
  .content-wrapper-with-tabs {
    max-width: 60% !important;
  }
}

/* 導覽列也要跟著變 90% */
:deep(.v-app-bar) {
  max-width: 60% !important;
}

/* 可編輯的成員名稱樣式 */
.member-name-editable {
  cursor: pointer;
  transition: all 0.2s;
}

.member-name-editable:hover {
  color: rgb(var(--v-theme-primary));
}
</style>
