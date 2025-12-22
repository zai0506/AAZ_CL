<template>
  <v-app>

    <!-- 左側導覽欄 -->
    <div class="sidebar">
      <!-- Logo -->
      <div class="sidebar-header">
        <h1 class="logo-title d-flex align-center justify-center mb-6" @click="router.push('/home')"
          style="cursor: pointer;">
          欸
          <img src="/AAZ_icon.png" alt="A" class="logo-icon" />
          誌
        </h1>
      </div>

      <!-- 群組名稱標題 -->
      <div class="nav-header">
        <v-list class="pa-0" bg-color="transparent">
          <div class="d-flex align-center">
            <v-btn icon="mdi-arrow-left-thick" variant="text" size="large" class="text-medium-emphasis"
              @click="router.push('/home')"></v-btn>
            <v-list-subheader class="pa-0">{{ group?.name || '載入中...' }}</v-list-subheader>
          </div>
          <v-divider class="my-3"></v-divider>
        </v-list>
      </div>

      <!-- 功能選單區域 -->
      <div class="nav-content">
        <v-list class="pa-0" bg-color="transparent">
          <v-list-item prepend-icon="mdi-format-list-text" title="明細" class="nav-item"
            :class="{ 'active-nav-item': currentTab === 'transactions' }"
            @click="currentTab = 'transactions'"></v-list-item>

          <v-list-item prepend-icon="mdi-account-switch-outline" title="結算" class="nav-item"
            :class="{ 'active-nav-item': currentTab === 'settlement' }"
            @click="currentTab = 'settlement'"></v-list-item>

          <v-list-item prepend-icon="mdi-chart-pie" title="統計" class="nav-item"
            :class="{ 'active-nav-item': currentTab === 'stats' }" @click="currentTab = 'stats'"></v-list-item>



          <v-list-item prepend-icon="mdi-information-variant-circle-outline" title="行程資訊" class="nav-item"
            :class="{ 'active-nav-item': currentTab === 'info' }" @click="currentTab = 'info'"></v-list-item>
        </v-list>
      </div>

      <!-- 底部使用者區 -->
      <v-menu location="top" v-model="menuOpen">
        <template v-slot:activator="{ props }">
          <v-list-item class="nav-item" v-bind="props" value="user-menu">
            <template v-slot:prepend>
              <v-avatar color="white" size="40">
                <span class="text-primary text-h6">{{ userInitial }}</span>
              </v-avatar>
            </template>
            <v-list-item-title class="font-weight-bold">{{ userStore.nickname }}</v-list-item-title>
            <template v-slot:append>
              <v-icon :icon="menuOpen ? 'mdi-chevron-down' : 'mdi-chevron-up'"></v-icon>
            </template>
          </v-list-item>
        </template>

        <v-list class="submenu-list">
          <v-list-item value="profile" prepend-icon="mdi-account-circle-outline" @click="handleProfileClick">
            <v-list-item-title>個人檔案</v-list-item-title>
          </v-list-item>
          <v-list-item value="logout" @click="handleLogout" prepend-icon="mdi-logout">
            <v-list-item-title>登出</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </div>

    <!-- 右側內容區 -->
    <v-main class="main-content">
      <!-- 白色內容區塊 -->
      <div class="content-wrapper-with-tabs">
        <v-container fluid class="pa-6">
          <v-window v-model="currentTab">
            <!-- Paperclip 控制按鈕 (永遠顯示) -->
            <v-tooltip location="bottom">
              <template v-slot:activator="{ props }">
                <v-icon v-bind="props" @click="showAnnouncement = !showAnnouncement" class="paperclip-icon"
                  style="position: fixed; top: 35px; right: 142px; color: #777; cursor: pointer; z-index: 10001;transform: rotate(2deg);">
                  mdi-paperclip
                </v-icon>
              </template>
              <span>{{ showAnnouncement ? '隱藏布告欄' : '顯示布告欄' }}</span>
            </v-tooltip>

            <!-- 浮貼布告欄 -->
            <transition name="announcement-fade">
              <div v-if="showAnnouncement" class="floating-announcement">
                <!-- 小飛機 icon -->
                <v-icon class="airplane-icon" size="20" color="#555555">mdi-airplane</v-icon>

                <v-img :src="`https://picsum.photos/seed/${group?.id}/400/200`" height="120" cover
                  style="width: 100%; border-radius: 2px;"></v-img>

                <div style="margin-top: 15px; font-family: 'cursive', 'Noto Sans TC'; color: #333; text-align: center;">
                  <div
                    style="font-weight: bold; border-bottom: 1px solid #56AB2F; display: inline-block; margin-bottom: 5px;">
                    大會報告📸</div>
                  <div style="font-size: 13px;">{{ group?.announcement || '暫無公告' }}</div>
                </div>
              </div>
            </transition>
            <!-- 交易明細頁面 -->
            <v-window-item value="transactions">
              <div class="narrow-content">
                <v-row>
                  <v-col cols="12">
                    <!-- 搜尋輸入框 -->
                    <div class="d-flex align-start gap-2 mb-4">
                      <div class="flex-grow-1">
                        <v-text-field v-model="searchKeyword" placeholder="搜尋交易內容（日期、品項、金額、貨幣、類別、成員、備註等）"
                          prepend-inner-icon="mdi-magnify" variant="outlined" density="compact" clearable
                          @click:clear="searchKeyword = ''" hide-details class="search-input"></v-text-field>
                      </div>
                      <div style="width: 48px;"></div>
                    </div>

                    <!-- 交易列表 -->
                    <div v-if="filteredTransactions.length > 0" class="d-flex align-start gap-2">
                      <v-card class="flex-grow-1 dashed-border-card">
                        <v-list>
                          <template v-for="(dateGroup, index) in groupedTransactions" :key="index">
                            <!-- 日期分組標題 -->
                            <v-list-subheader class="date-group-header">
                              {{ dateGroup.date }}
                            </v-list-subheader>

                            <!-- 該日期的交易項目 -->
                            <v-list-item v-for="transaction in dateGroup.transactions" :key="transaction.transactionId"
                              @click="viewTransaction(transaction)" class="transaction-item">
                              <!-- 圖示 -->
                              <template v-slot:prepend>
                                <v-avatar :color="getTransactionColor(transaction.type)">
                                  <v-icon color="white">{{
                                    getCategoryIcon(transaction)
                                    }}</v-icon>
                                </v-avatar>
                              </template>

                              <!-- 內容 -->
                              <div style="flex: 1;">
                                <v-list-item-title class="font-weight-bold">
                                  {{ transaction.title }}
                                </v-list-item-title>
                                <v-list-item-subtitle>
                                  <div v-if="transaction.type !== 'transfer'">
                                    {{ getPayersOrReceiversText(transaction) }}
                                  </div>
                                </v-list-item-subtitle>
                              </div>

                              <!-- 金額 -->
                              <template v-slot:append>
                                <div class="text-right">
                                  <div class="font-weight-bold" :class="getAmountColor(transaction.type)">
                                    {{ formatAmount(transaction.amount, transaction.currency) }}
                                  </div>
                                  <div
                                    v-if="transaction.convertedAmount && transaction.currency !== group?.baseCurrency"
                                    class="text-caption" :class="getAmountColor(transaction.type)">
                                    ≈ {{ formatAmount(transaction.convertedAmount, group?.baseCurrency) }}
                                  </div>
                                </div>
                              </template>
                            </v-list-item>
                          </template>
                        </v-list>
                      </v-card>

                      <!-- 篩選按鈕 -->
                      <v-menu location="bottom end">
                        <template v-slot:activator="{ props }">
                          <v-btn icon="mdi-filter-variant" variant="text" color="white" v-bind="props"></v-btn>
                        </template>
                        <v-list class="filter-menu">
                          <v-list-item @click="sortTransactions('date-desc')"
                            :class="{ 'active-filter': transactionSort === 'date-desc' }">
                            <template v-slot:prepend>
                              <v-icon>mdi-sort-calendar-descending</v-icon>
                            </template>
                            <v-list-item-title>日期新到舊</v-list-item-title>
                          </v-list-item>
                          <v-list-item @click="sortTransactions('date-asc')"
                            :class="{ 'active-filter': transactionSort === 'date-asc' }">
                            <template v-slot:prepend>
                              <v-icon>mdi-sort-calendar-ascending</v-icon>
                            </template>
                            <v-list-item-title>日期舊到新</v-list-item-title>
                          </v-list-item>
                          <v-list-item @click="sortTransactions('amount-desc')"
                            :class="{ 'active-filter': transactionSort === 'amount-desc' }">
                            <template v-slot:prepend>
                              <v-icon>mdi-sort-numeric-descending</v-icon>
                            </template>
                            <v-list-item-title>金額高到低</v-list-item-title>
                          </v-list-item>
                          <v-list-item @click="sortTransactions('amount-asc')"
                            :class="{ 'active-filter': transactionSort === 'amount-asc' }">
                            <template v-slot:prepend>
                              <v-icon>mdi-sort-numeric-ascending</v-icon>
                            </template>
                            <v-list-item-title>金額低到高</v-list-item-title>
                          </v-list-item>
                          <v-list-item @click="sortTransactions('currency')"
                            :class="{ 'active-filter': transactionSort === 'currency' }">
                            <template v-slot:prepend>
                              <v-icon>mdi-currency-usd</v-icon>
                            </template>
                            <v-list-item-title>按幣別排序</v-list-item-title>
                          </v-list-item>
                        </v-list>
                      </v-menu>
                    </div>

                    <!-- 空狀態 -->
                    <div v-else-if="transactions.length === 0"
                      class="d-flex flex-column align-center justify-center text-center" style="min-height: 70vh;">
                      <v-icon size="128" color="grey-lighten-2" class="mb-4">mdi-receipt-text-outline</v-icon>
                      <p class="text-h5 text-grey-darken-1">還沒有交易記錄</p>
                      <p class="text-body-1 text-grey-darken-1">點擊下方「+」按鈕新增第一筆交易</p>
                    </div>

                    <!-- 搜尋無結果 -->
                    <div v-else class="d-flex flex-column align-center justify-center text-center"
                      style="min-height: 70vh;">
                      <v-icon size="128" color="grey-lighten-2" class="mb-4">mdi-magnify-close</v-icon>
                      <p class="text-h5 text-grey-darken-1">找不到符合的交易記錄</p>
                      <p class="text-body-1 text-grey-darken-1">請嘗試其他關鍵字</p>
                    </div>
                  </v-col>
                </v-row>
              </div>
            </v-window-item>

            <!-- 行程資訊頁面 -->
            <v-window-item value="info">
              <div class="narrow-content">
                <v-card v-if="group" class="dashed-border-card">
                  <v-card-title class="d-flex align-center">
                    <v-spacer></v-spacer>
                    <template v-if="!isGroupInfoEditing">
                      <v-btn icon="mdi-pencil" variant="text" @click="startGroupInfoEdit"></v-btn>
                    </template>
                    <template v-else>
                      <v-btn variant="text" @click="cancelGroupInfoEdit">取消</v-btn>
                      <v-btn :color="'rgb(85, 214, 194)'" class="text-white" variant="elevated"
                        @click="saveGroupInfoChanges" :disabled="!formValid || !!dateErrorMessage">儲存</v-btn>
                    </template>
                  </v-card-title>
                  <v-card-text>
                    <v-form ref="groupInfoForm" v-model="formValid">
                      <v-row>
                        <v-col cols="12">
                          <h3 class="text-h6 mb-2">行程名稱</h3>
                          <p v-if="!isGroupInfoEditing">{{ group.name }}</p>
                          <v-text-field v-else v-model="editingGroup.name" placeholder="請輸入行程名稱"
                            hide-details></v-text-field>
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
                            <v-text-field v-model="editingGroup.startDate" type="date" density="compact" hide-details
                              :error="!!dateErrorMessage"></v-text-field>
                            <span class="mx-2">-</span>
                            <v-text-field v-model="editingGroup.endDate" type="date" density="compact" hide-details
                              :error="!!dateErrorMessage"></v-text-field>
                          </div>
                        </v-col>

                        <v-col cols="12">
                          <h3 class="text-h6 mb-2">主要貨幣</h3>
                          <p v-if="!isGroupInfoEditing">{{ group.baseCurrency }}</p>
                          <v-select v-else v-model="editingGroup.baseCurrency" :items="currencies" density="compact"
                            hide-details readonly></v-select>
                        </v-col>

                        <v-col cols="12">
                          <div class="d-flex align-center justify-space-between mb-2">
                            <h3 class="text-h6">成員列表</h3>
                            <!-- 編輯模式時，新增成員按鈕放在右邊 -->
                            <v-btn v-if="isGroupInfoEditing" prepend-icon="mdi-account-plus" color="secondary"
                              variant="tonal" size="small" @click="addNewMemberField" :disabled="isAddMemberDisabled">
                              新增成員
                            </v-btn>
                          </div>

                          <!-- 非編輯模式：垂直列表顯示 -->
                          <v-list v-if="!isGroupInfoEditing" density="compact">
                            <v-list-item v-for="member in sortedMembers" :key="member.id"
                              :prepend-icon="member.isCreator ? 'mdi-crown' : 'mdi-account'">
                              <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                              <template v-slot:append v-if="member.isCreator">
                                <v-chip color="primary" size="small">建立者</v-chip>
                              </template>
                            </v-list-item>
                          </v-list>

                          <!-- 編輯模式：可編輯的垂直列表 -->
                          <v-list v-else density="compact">
                            <v-list-item v-for="member in sortedMembers" :key="member.id"
                              :prepend-icon="member.isCreator ? 'mdi-crown' : 'mdi-account'">
                              <!-- Creator 不可編輯 -->
                              <v-list-item-title v-if="member.isCreator">
                                {{ member.displayName }}
                              </v-list-item-title>

                              <!-- 正在編輯的成員：顯示輸入框 -->
                              <v-text-field v-else-if="editingMembers[member.id]?.isEditing"
                                v-model="editingMembers[member.id].newName" @blur="finishEditMember(member.id)"
                                @keyup.enter="finishEditMember(member.id)" density="compact" hide-details
                                autofocus></v-text-field>

                              <!-- 未編輯或編輯完成的成員：可點擊 -->
                              <v-list-item-title v-else class="member-name-editable" @click="startEditMember(member)">
                                {{ editingMembers[member.id]?.newName || member.displayName }}
                              </v-list-item-title>

                              <template v-slot:append>
                                <!-- Creator 標籤 -->
                                <v-chip v-if="member.isCreator" color="primary" size="small">建立者</v-chip>
                                <!-- 已編輯標記 -->
                                <v-icon v-else-if="
                                  editingMembers[member.id] && !editingMembers[member.id].isEditing
                                " color="success">mdi-check-circle</v-icon>
                                <!-- 可編輯提示 -->
                                <v-icon v-else-if="!editingMembers[member.id]?.isEditing" size="small"
                                  color="grey-lighten-1">mdi-pencil</v-icon>
                              </template>
                            </v-list-item>

                            <!-- 動態新增的成員輸入框 -->
                            <v-list-item v-for="(member, index) in newMembers" :key="member.tempId"
                              prepend-icon="mdi-account-plus">
                              <v-text-field v-model="member.name" placeholder="輸入成員名稱" density="compact" hide-details
                                @blur="handleNewMemberBlur(member)" :ref="(el) => {
                                  if (index === newMembers.length - 1) latestMemberInput = el;
                                }
                                  "></v-text-field>
                            </v-list-item>
                          </v-list>
                        </v-col>

                        <v-col cols="12">
                          <h3 class="text-h6 mb-2">公告欄</h3>
                          <p v-if="!isGroupInfoEditing">{{ group.announcement || '無' }}</p>
                          <v-textarea v-else v-model="editingGroup.announcement" placeholder="請輸入公告內容" rows="3"
                            density="compact" hide-details></v-textarea>
                        </v-col>
                      </v-row>
                    </v-form>

                    <!-- 刪除行程按鈕 -->
                    <div v-if="!isGroupInfoEditing" class="d-flex justify-center mt-6">
                      <v-btn color="red" variant="elevated" prepend-icon="mdi-delete"
                        @click="showDeleteGroupConfirm = true" style="width: 90%;">
                        刪除行程
                      </v-btn>
                    </div>
                  </v-card-text>
                </v-card>
              </div>
            </v-window-item>

            <!-- 結算頁面 -->
            <v-window-item value="settlement">
              <div class="narrow-content">
                <v-row>
                  <v-col cols="12">
                    <!-- 空狀態 -->
                    <div v-if="balanceReport && nonZeroBalances.length === 0"
                      class="d-flex flex-column align-center justify-center text-center" style="min-height: 70vh;">
                      <v-icon size="120" color="grey-lighten-2">mdi-check-circle-outline</v-icon>
                      <p class="text-h5 mt-4 text-grey-darken-1">目前無結算項目</p>
                      <p class="text-body-1 text-grey-darken-1">所有款項已結清</p>
                    </div>

                    <!-- 有餘額時顯示卡片 -->
                    <v-card v-else-if="balanceReport && nonZeroBalances.length > 0" class="dashed-border-card">
                      <!-- 餘額長條圖 -->
                      <v-card-title>成員餘額</v-card-title>
                      <v-card-text>
                        <div v-for="balance in nonZeroBalances" :key="balance.memberId" class="mb-4">
                          <div class="d-flex justify-space-between mb-1">
                            <span>{{ balance.memberName }}</span>
                            <span :class="balance.balance >= 0 ? 'text-green' : 'text-red'">
                              {{ formatAmount(Math.abs(balance.balance), group?.baseCurrency) }}
                            </span>
                          </div>
                          <v-progress-linear :model-value="Math.abs(balance.balance)" :max="maxBalance"
                            :color="balance.balance >= 0 ? 'green' : 'red'" height="20"></v-progress-linear>
                        </div>
                      </v-card-text>

                      <v-divider></v-divider>

                      <!-- 結算建議 -->
                      <v-card-title>結算方案</v-card-title>
                      <v-card-text>
                        <v-list v-if="balanceReport.debts && balanceReport.debts.length > 0">
                          <v-list-item v-for="(debt, index) in balanceReport.debts" :key="index">
                            <v-list-item-title>
                              <v-icon color="#FF0000">mdi-arrow-right-bold</v-icon>
                              <strong>{{ debt.fromMemberName }}</strong> 尚欠
                              <strong>{{ debt.toMemberName }}</strong>
                            </v-list-item-title>
                            <template v-slot:append>
                              <div class="d-flex align-center">
                                <v-chip color="primary">
                                  {{ formatAmount(debt.amount, group?.baseCurrency) }}
                                </v-chip>
                                <v-btn variant="tonal" color="yellow-darken-2" size="small" class="ml-4"
                                  @click="openTransferFromSettlement(debt)">
                                  匯款去
                                </v-btn>
                              </div>
                            </template>
                          </v-list-item>
                        </v-list>
                        <p v-else class="text-center grey--text py-4">目前沒有需要結算的項目</p>
                      </v-card-text>
                    </v-card>
                  </v-col>
                </v-row>
              </div>
            </v-window-item>

            <!-- 統計頁面 -->
            <v-window-item value="stats">
              <div v-if="expenseStats.total === 0 && incomeStats.total === 0"
                class="d-flex flex-column align-center justify-center text-center" style="min-height: 70vh;">
                <v-icon size="128" color="grey-lighten-2" class="mb-4">mdi-plus-minus-variant</v-icon>
                <p class="text-h5 text-grey-darken-1">尚無統計數據</p>
                <p class="text-body-1 text-grey-darken-1">新增交易後即可查看統計圖表</p>
              </div>
              <div v-else>
                <!-- 統計期間顯示 -->
                <v-row class="mb-4">
                  <v-col cols="12">
                    <div class="d-flex align-center">
                      <span class="text-subtitle-1">
                        統計期間：{{ formatStatsDateRange() }}
                      </span>
                      <v-btn color="primary" variant="text" size="small" class="ml-4" @click="openStatsDateDialog">
                        設定期間
                      </v-btn>
                    </div>
                  </v-col>
                </v-row>

                <v-row>
                  <v-col cols="12" md="6">
                    <v-card class="dashed-border-card">
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
                        <div v-for="cat in expenseStats.categoryStats || []" :key="cat.category"
                          class="mb-3 category-bar" :class="{ 'hovered': hoveredCategory === cat.category }"
                          @mouseenter="hoveredCategory = cat.category" @mouseleave="hoveredCategory = null"
                          @click="openCategoryDetail(cat.category, 'expense')" style="cursor: pointer;">
                          <div class="d-flex justify-space-between align-center">
                            <v-icon size="small" class="mr-2">{{ getCategoryIcon(cat.category) }}</v-icon>
                            <span>{{ getCategoryName(cat.category) }}</span>
                            <span>{{ cat.percentage?.toFixed(1) }}%</span>
                          </div>
                          <v-progress-linear :model-value="cat.percentage || 0" :color="getCategoryColor(cat.category)"
                            height="8"></v-progress-linear>
                        </div>
                      </v-card-text>
                    </v-card>
                  </v-col>

                  <v-col cols="12" md="6">
                    <v-card class="dashed-border-card">
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
                        <div v-for="cat in incomeStats.categoryStats || []" :key="cat.category"
                          class="mb-3 category-bar" :class="{ 'hovered': hoveredCategory === cat.category }"
                          @mouseenter="hoveredCategory = cat.category" @mouseleave="hoveredCategory = null"
                          @click="openCategoryDetail(cat.category, 'income')" style="cursor: pointer;">
                          <div class="d-flex justify-space-between align-center">
                            <v-icon size="small" class="mr-2">{{ getCategoryIcon(cat.category) }}</v-icon>
                            <span>{{ getCategoryName(cat.category) }}</span>
                            <span>{{ cat.percentage?.toFixed(1) }}%</span>
                          </div>
                          <v-progress-linear :model-value="cat.percentage || 0" :color="getCategoryColor(cat.category)"
                            height="8"></v-progress-linear>
                        </div>
                      </v-card-text>
                    </v-card>
                  </v-col>
                </v-row>
              </div>
            </v-window-item>
          </v-window>
        </v-container>
      </div>
    </v-main>

    <!-- ========== 浮動新增按鈕 (選單) ========== -->
    <v-menu location="top center">
      <template v-slot:activator="{ props }">
        <v-btn v-if="currentTab === 'transactions'" size="x-large" icon
          style="position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 999; background-color: rgb(85, 214, 194);"
          elevation="8" v-bind="props">
          <v-icon color="white">mdi-plus</v-icon>
        </v-btn>
      </template>
      <v-list class="add-transaction-menu">
        <v-list-item class="add-expense-item" @click="openAddModal('expense')" prepend-icon="mdi-cart-outline"
          title="新增支出"></v-list-item>
        <v-list-item class="add-income-item" @click="openAddModal('income')" prepend-icon="mdi-cash-plus"
          title="新增收入"></v-list-item>
        <v-list-item class="add-transfer-item" @click="openAddModal('transfer')" prepend-icon="mdi-bank-transfer"
          title="新增轉帳"></v-list-item>
      </v-list>
    </v-menu>

    <!-- ========== 交易 Modal ========== -->
    <!-- ========== 交易 Modals ========== -->
    <ExpenseModal v-model="showExpenseModal" :trip-id="groupId" :members="group?.members"
      :base-currency="group?.baseCurrency" :transaction="selectedTransaction" @refresh="refreshAllData" />

    <IncomeModal v-model="showIncomeModal" :trip-id="groupId" :members="group?.members"
      :base-currency="group?.baseCurrency" :transaction="selectedTransaction" @refresh="refreshAllData" />

    <TransferModal v-model="showTransferModal" :trip-id="groupId" :members="group?.members"
      :base-currency="group?.baseCurrency" :transaction="selectedTransaction" @refresh="refreshAllData" />

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
              <v-list-item v-for="transaction in dateGroup.transactions" :key="transaction.transactionId"
                class="transaction-item" @click="viewTransaction(transaction)">
                <div style="flex: 1;">
                  <v-list-item-title>{{ transaction.title }}</v-list-item-title>
                  <v-list-item-subtitle>
                    <div v-if="transaction.type !== 'transfer'">
                      {{ getPayersOrReceiversText(transaction) }}
                    </div>
                  </v-list-item-subtitle>
                </div>
                <template v-slot:append>
                  <div class="text-right">
                    <div class="font-weight-bold" :class="getAmountColor(transaction.type)">
                      {{ formatAmount(transaction.amount, transaction.currency) }}
                    </div>
                    <div v-if="transaction.convertedAmount && transaction.currency !== group?.baseCurrency"
                      class="text-caption grey--text">
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
              <v-text-field v-model="tempStatsStartDate" label="開始日期" type="date" prepend-icon="mdi-calendar"
                density="comfortable" :error="!!statsDateErrorMessage"></v-text-field>
            </v-col>
            <v-col cols="12">
              <v-text-field v-model="tempStatsEndDate" label="結束日期" type="date" prepend-icon="mdi-calendar"
                density="comfortable" :error="!!statsDateErrorMessage"></v-text-field>
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

    <!-- 刪除行程確認對話框 -->
    <v-dialog v-model="showDeleteGroupConfirm" max-width="400px">
      <v-card>
        <v-alert type="warning" variant="tonal" class="mb-3">
          此操作無法復原！
        </v-alert>
        <v-card-title class="text-h6" style="padding-left: 40px;">確定要刪除 {{ group?.name }} 嗎？</v-card-title>
        <v-card-text>
          <p class="text-body-1 text-grey-darken-1 pl-4">提醒您：<br>刪除後將永久移除行程的所有資料及紀錄。</p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="red" variant="elevated" @click="deleteGroup" :loading="deletingGroup">
            確定
          </v-btn>
          <v-btn variant="text" @click="showDeleteGroupConfirm = false">取消</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

  </v-app>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { useProfileDialogStore } from '@/stores/profileDialog';
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
const profileDialogStore = useProfileDialogStore();

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

// 交易排序
const transactionSort = ref('date-desc'); // 'date-desc', 'date-asc', 'amount-desc', 'amount-asc'

// 搜尋關鍵字
const searchKeyword = ref('');

// 布告欄顯示狀態
const showAnnouncement = ref(true);

// 刪除行程
const showDeleteGroupConfirm = ref(false);
const deletingGroup = ref(false);

// 側邊欄狀態
const menuOpen = ref(false);
const userInitial = computed(() => {
  return userStore.nickname ? userStore.nickname.charAt(0).toUpperCase() : 'U';
});

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

// 從結算建議開啟轉帳 Modal
const openTransferFromSettlement = (debt) => {
  if (!group.value?.members) return;

  const fromMember = group.value.members.find(m => m.displayName === debt.fromMemberName);
  const toMember = group.value.members.find(m => m.displayName === debt.toMemberName);

  if (!fromMember || !toMember) {
    console.error("無法從結算建議中找到成員:", debt);
    alert('無法找到對應的轉帳成員。');
    return;
  }

  selectedTransaction.value = {
    type: 'transfer',
    fromMemberId: fromMember.id,
    toMemberId: toMember.id,
    amount: debt.amount,
    currency: group.value.baseCurrency,
    transactionDate: new Date().toISOString().split('T')[0], // 預設為今天
    title: '結清款項', // 預設標題
  };
  showTransferModal.value = true;
};

// 設定交易排序方式
const sortTransactions = (sortType) => {
  transactionSort.value = sortType;
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

// 根據搜尋關鍵字篩選交易
const filteredTransactions = computed(() => {
  if (!transactions.value || transactions.value.length === 0) return [];
  if (!searchKeyword.value.trim()) return transactions.value;

  const keyword = searchKeyword.value.trim().toLowerCase();

  return transactions.value.filter(transaction => {
    // 1. 搜尋日期（支援 / 分隔格式，例如 12/20 或 2024/12/20）
    if (transaction.transactionDate) {
      const date = new Date(transaction.transactionDate);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');

      // 格式化為多種格式供搜尋
      const dateFormats = [
        `${year}/${month}/${day}`,  // 2024/12/20
        `${month}/${day}`,           // 12/20
        `${year}${month}${day}`,     // 20241220
        transaction.transactionDate.toLowerCase() // 原始格式
      ];

      if (dateFormats.some(format => format.includes(keyword))) {
        return true;
      }
    }

    // 2. 搜尋品項名稱（title）
    if (transaction.title && transaction.title.toLowerCase().includes(keyword)) {
      return true;
    }

    // 3. 搜尋金額（轉為字串後搜尋）
    if (transaction.amount && transaction.amount.toString().includes(keyword)) {
      return true;
    }

    // 4. 搜尋轉換後金額
    if (transaction.convertedAmount && transaction.convertedAmount.toString().includes(keyword)) {
      return true;
    }

    // 5. 搜尋原始交易貨幣
    if (transaction.currency && transaction.currency.toLowerCase().includes(keyword)) {
      return true;
    }

    // 6. 搜尋類別
    if (transaction.category && transaction.category.toLowerCase().includes(keyword)) {
      return true;
    }

    // 7. 搜尋備註
    if (transaction.notes && transaction.notes.toLowerCase().includes(keyword)) {
      return true;
    }

    // 8. 搜尋付款人（payments）
    if (transaction.payments && Array.isArray(transaction.payments)) {
      const payerNames = transaction.payments.map(p => {
        const member = group.value?.members?.find(m => m.id === p.memberId);
        return member?.displayName || '';
      }).join(' ').toLowerCase();

      if (payerNames.includes(keyword)) {
        return true;
      }
    }

    // 9. 搜尋分攤者（splits）
    if (transaction.splits && Array.isArray(transaction.splits)) {
      const splitterNames = transaction.splits.map(s => {
        const member = group.value?.members?.find(m => m.id === s.memberId);
        return member?.displayName || '';
      }).join(' ').toLowerCase();

      if (splitterNames.includes(keyword)) {
        return true;
      }
    }

    // 10. 搜尋轉帳相關成員（fromMemberId, toMemberId）
    if (transaction.type === 'transfer') {
      if (transaction.fromMemberId) {
        const fromMember = group.value?.members?.find(m => m.id === transaction.fromMemberId);
        if (fromMember?.displayName && fromMember.displayName.toLowerCase().includes(keyword)) {
          return true;
        }
      }
      if (transaction.toMemberId) {
        const toMember = group.value?.members?.find(m => m.id === transaction.toMemberId);
        if (toMember?.displayName && toMember.displayName.toLowerCase().includes(keyword)) {
          return true;
        }
      }
    }

    return false;
  });
});

// 按日期分組交易（明細頁面）
const groupedTransactions = computed(() => {
  if (!filteredTransactions.value || filteredTransactions.value.length === 0) return [];

  let sortedTransactions = [...filteredTransactions.value];

  // 根據排序類型決定排序方式
  if (transactionSort.value === 'date-desc') {
    // 日期新到舊
    sortedTransactions.sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));
  } else if (transactionSort.value === 'date-asc') {
    // 日期舊到新
    sortedTransactions.sort((a, b) => new Date(a.transactionDate) - new Date(b.transactionDate));
  } else if (transactionSort.value === 'amount-desc') {
    // 金額高到低
    sortedTransactions.sort((a, b) => b.amount - a.amount);
  } else if (transactionSort.value === 'amount-asc') {
    // 金額低到高
    sortedTransactions.sort((a, b) => a.amount - b.amount);
  } else if (transactionSort.value === 'currency') {
    // 按幣別排序：主要貨幣優先，其他按A-Z
    const baseCurrency = group.value?.baseCurrency || '';

    sortedTransactions.sort((transA, transB) => {
      const currencyA = transA.currency || '';
      const currencyB = transB.currency || '';

      // 如果 A 是主要貨幣，A 排前面
      if (currencyA === baseCurrency && currencyB !== baseCurrency) return -1;
      // 如果 B 是主要貨幣，B 排前面
      if (currencyB === baseCurrency && currencyA !== baseCurrency) return 1;
      // 兩者都是或都不是主要貨幣，按字母順序排序
      return currencyA.localeCompare(currencyB);
    });
  } else {
    // 預設：日期降序
    sortedTransactions.sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));
  }

  // 根據排序類型決定分組方式
  if (transactionSort.value === 'date-desc' || transactionSort.value === 'date-asc') {
    // 日期排序：傳統的日期分組方式
    const groups = {};
    sortedTransactions.forEach(transaction => {
      const dateKey = transaction.transactionDate.split('T')[0];
      if (!groups[dateKey]) {
        groups[dateKey] = [];
      }
      groups[dateKey].push(transaction);
    });

    const dateOrder = [];
    sortedTransactions.forEach(transaction => {
      const dateKey = transaction.transactionDate.split('T')[0];
      if (!dateOrder.includes(dateKey)) {
        dateOrder.push(dateKey);
      }
    });

    return dateOrder.map(dateKey => ({
      date: formatDateFull(dateKey),
      transactions: groups[dateKey]
    }));
  } else {
    // 金額或貨幣排序：按排序順序分組，只在日期改變時顯示日期標題
    const result = [];
    let currentDateKey = null;
    let currentGroup = null;

    sortedTransactions.forEach(transaction => {
      const dateKey = transaction.transactionDate.split('T')[0];

      if (dateKey !== currentDateKey) {
        // 日期改變，創建新的日期組
        if (currentGroup) {
          result.push(currentGroup);
        }
        currentDateKey = dateKey;
        currentGroup = {
          date: formatDateFull(dateKey),
          transactions: [transaction]
        };
      } else {
        // 同一日期，加入現有組
        currentGroup.transactions.push(transaction);
      }
    });

    // 加入最後一個組
    if (currentGroup) {
      result.push(currentGroup);
    }

    return result;
  }
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
    expense: '#E67E66',
    income: '#4874299b',
    transfer: '#FFB800',
  };
  return colors[type] || 'grey';
};

// 取得金額顏色
const getAmountColor = (type) => {
  return type === 'expense' ? 'text-red' : type === 'income' ? 'text-green' : 'text-blue';
};

// 取得付款人或收款人文字
const getPayersOrReceiversText = (transaction) => {
  if (transaction.type === 'expense' && transaction.payments && transaction.payments.length > 0) {
    // 支出：顯示付款人
    const payerNames = transaction.payments.map(p => {
      const member = group.value?.members?.find(m => m.id === p.memberId);
      return member?.displayName || '';
    }).filter(name => name !== '');

    if (payerNames.length > 0) {
      return payerNames.join(', ') + ' 先付';
    }
  } else if (transaction.type === 'income' && transaction.payments && transaction.payments.length > 0) {
    // 收入：顯示收款人
    const receiverNames = transaction.payments.map(p => {
      const member = group.value?.members?.find(m => m.id === p.memberId);
      return member?.displayName || '';
    }).filter(name => name !== '');

    if (receiverNames.length > 0) {
      return receiverNames.join(', ') + ' 先收';
    }
  }
  return '';
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
        label: function (context) {
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
        label: function (context) {
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

// 打開個人檔案
const handleProfileClick = () => {
  menuOpen.value = false;
  profileDialogStore.openDialog();
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

// 判斷當前用戶是否為建立者
const isCurrentUserCreator = computed(() => {
  if (!group.value?.members || !userStore.id) return false;
  const currentUserMember = group.value.members.find(
    (m) => m.userId === parseInt(userStore.id)
  );
  return currentUserMember?.isCreator || false;
});

// 刪除行程
const deleteGroup = async () => {
  deletingGroup.value = true;

  try {
    const response = await axios.delete(`/trips/${groupId.value}`, {
      skipAutoLogout: true, // 避免 401 時自動登出
    });
    console.log('刪除成功:', response);
    showDeleteGroupConfirm.value = false;
    // 刪除成功後導航回首頁
    router.push('/home');
  } catch (error) {
    console.error('刪除行程失敗:', error);
    console.error('錯誤詳情:', error.response?.data);

    if (error.response?.status === 401) {
      alert('刪除失敗：此功能的後端 API 尚未實現或您沒有權限。\n請確認後端是否已實現 DELETE /trips/{id} 端點。');
    } else {
      alert('刪除失敗：' + (error.response?.data?.message || error.message));
    }
  } finally {
    deletingGroup.value = false;
  }
};

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
  display: flex;
  flex-direction: column;
  padding: 32px 24px;
}

.sidebar-header {
  text-align: center;
  padding-bottom: 24px;
  margin-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
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
  cursor: pointer;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

/* 選中狀態的樣式 */
.active-nav-item {
  background-color: rgb(85, 214, 194) !important;
  color: white !important;
}

.active-nav-item .v-icon,
.active-nav-item .v-list-item-title {
  color: white !important;
}

/* 覆寫 Vuetify 預設樣式 */
.v-list-subheader {
  color: rgba(0, 0, 0, 0.6);
  font-weight: bold;
  padding-left: 16px;
  font-size: 1.6em;
  padding-top: 8px;
  padding-bottom: 8px;
  line-height: 1.2;
}

.v-list-item {
  border-radius: 8px !important;
  font-size: 1.4em;
}

.nav-item:hover {
  background-color: rgb(85, 214, 194) !important;
  color: white !important;
}

.nav-item:hover .v-icon,
.nav-item:hover .v-list-item-title {
  color: white !important;
}

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

/* ========== 右側內容區樣式 ========== */
.main-content {
  position: relative;
  z-index: 1;
  margin-left: 20%;
  width: 80%;
  min-height: 100vh;
  padding: 0;
}

/* ========== 交易列表樣式 ========== */
.transaction-item {
  border-bottom: 1px solid #e0e0e0;
  cursor: pointer;
  align-items: center !important;
}

.transaction-item:hover {
  background-color: #f5f5f5;
}

/* 確保交易項目內容區域可以正確對齊 */
.transaction-item>div {
  display: flex;
  flex-direction: column;
  justify-content: center;
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

:deep(.text-red) {
  color: #E67E66 !important;
}

:deep(.text-green) {
  color: #4874299b !important;
}

:deep(.text-blue) {
  color: #FFB800 !important;
}

/* GroupDetailView 專用：內容容器 */
.content-wrapper-with-tabs {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 100%;
  margin: 0 !important;
  background-color: rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.content-wrapper-with-tabs .v-container {
  flex: 1;
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

/* 明細、結算、行程資訊區塊寬度限制 */
.narrow-content {
  max-width: 60%;
  margin: 0 auto 0 10%;
  /* 左邊 10%，右邊自動 */
}

.add-transaction-menu {
  border-radius: 8px !important;
}

/* 篩選選單樣式 */
.filter-menu {
  border-radius: 8px !important;
  min-width: 180px;
}

.filter-menu .v-list-item {
  cursor: pointer;
  transition: all 0.2s;
}

.filter-menu .v-list-item:hover {
  background-color: rgba(85, 214, 194, 0.1);
}

.filter-menu .v-list-item.active-filter {
  background-color: rgba(85, 214, 194, 0.2);
  color: rgb(85, 214, 194);
}

.filter-menu .v-list-item.active-filter .v-icon {
  color: rgb(85, 214, 194);
}



.add-expense-item:hover {

  background-color: #E67E66 !important;

  color: white !important;

}

.add-expense-item:hover .v-icon,

.add-expense-item:hover .v-list-item-title {

  color: white !important;

}



.add-income-item:hover {

  background-color: #4874299b !important;

  color: white !important;

}

.add-income-item:hover .v-icon,

.add-income-item:hover .v-list-item-title {

  color: white !important;

}



.add-transfer-item:hover {

  background-color: #FFB800 !important;

  color: white !important;

}

.add-transfer-item:hover .v-icon,

.add-transfer-item:hover .v-list-item-title {

  color: white !important;

}

/* 搜尋框樣式 */
.search-input :deep(.v-field) {
  background-color: rgba(250, 250, 250, 0.4) !important;
}

.search-input :deep(.v-field--focused) {
  background-color: rgba(250, 250, 250, 0.4) !important;
}

/* 浮貼布告欄樣式 */
.floating-announcement {
  position: fixed;
  top: 50px;
  right: 30px;
  width: 240px;
  z-index: 10000 !important;
  background: #fff;
  padding: 12px 12px 30px 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  transform: rotate(2deg);
  outline: 3px dashed #FFB800;
  outline-offset: 0.1px;
  border-radius: 3px;
  transition: all 0.3s ease;
}

/* .floating-announcement:hover {
  transform: rotate(3deg) scale(1.1);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
} */

.paperclip-icon {
  transition: transform 0.2s ease;
}

.paperclip-icon:hover {
  transform: rotate(15deg) scale(1.2) !important;
}

.airplane-icon {
  position: absolute;
  top: 25px;
  left: -12px;
  transform: rotate(-15deg);
}

/* 布告欄顯示/隱藏動畫 */
.announcement-fade-enter-active {
  animation: announcement-in 0.5s ease-out;
}

.announcement-fade-leave-active {
  animation: announcement-out 0.4s ease-in;
}

@keyframes announcement-in {
  0% {
    opacity: 0;
    transform: rotate(-10deg) scale(0.5) translateY(-30px);
  }

  50% {
    transform: rotate(4deg) scale(1.05);
  }

  100% {
    opacity: 1;
    transform: rotate(2deg) scale(1);
  }
}

@keyframes announcement-out {
  0% {
    opacity: 1;
    transform: rotate(2deg) scale(1);
  }

  50% {
    transform: rotate(-5deg) scale(0.9);
  }

  100% {
    opacity: 0;
    transform: rotate(-15deg) scale(0.3) translateY(-50px);
  }
}
</style>
