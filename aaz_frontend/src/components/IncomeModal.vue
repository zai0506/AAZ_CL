<template>
  <v-dialog v-model="dialog" max-width="600px" persistent>
    <v-card class="modal-card">
      <v-card-title class="sticky-header d-flex align-center justify-space-between text-white" :style="{ backgroundColor: headerColor }">
        <span class="text-h6">{{ title }}</span>
        <v-btn icon="mdi-close" variant="text" size="small" color="white" @click="closeDialog"></v-btn>
      </v-card-title>

      <v-card-text class="scrollable-content pt-6">
        <v-form ref="form" v-model="valid">
          <!-- 日期 -->
          <v-text-field
            v-model="formData.incomeDate"
            label="日期"
            type="date"
            prepend-icon="mdi-calendar"
            :rules="[rules.required]"
            :readonly="isViewMode"
            required
            density="comfortable"
          ></v-text-field>

          <!-- 類別 -->
          <v-select
            v-model="formData.category"
            :items="categorySelectItems"
            label="類別"
            prepend-icon="mdi-tag"
            :rules="[rules.required]"
            :readonly="isViewMode"
            required
            density="comfortable"
          ></v-select>

          <!-- 品項 -->
          <v-text-field
            v-model="formData.title"
            label="品項"
            prepend-icon="mdi-list-box-outline"
            :rules="[rules.required]"
            :placeholder="currentPlaceholder"
            :readonly="isViewMode"
            required
            density="comfortable"
          ></v-text-field>

          <!-- 貨幣和金額 -->
          <v-row>
            <v-col cols="4">
              <v-select
                v-model="formData.currency"
                :items="currencies"
                label="貨幣"
                :rules="[rules.required]"
                @update:modelValue="handleCurrencyChange"
                :readonly="isViewMode"
                required
                density="comfortable"
                prepend-icon="mdi-currency-usd"
              ></v-select>
            </v-col>
            <v-col cols="8">
              <v-text-field
                v-model="formData.amount"
                label="金額"
                type="number"
                prepend-icon="mdi-circle-small"
                class="invisible-icon"
                :rules="[rules.required, rules.positive]"
                :readonly="isViewMode"
                required
                density="comfortable"
              ></v-text-field>
            </v-col>
          </v-row>

          <!-- 匯率和換算後金額 -->
          <v-row v-if="showExchangeRate">
            <v-col cols="4">
              <v-text-field
                v-model="formData.exchangeRate"
                label="匯率"
                type="number"
                step="0.0001"
                :hint="
                  rateSource === 'group'
                    ? '群組紀錄'
                    : rateSource === 'default'
                    ? '預設匯率'
                    : `1 ${formData.currency} / ${baseCurrency}`
                "
                :rules="[rules.required, rules.positive]"
                :loading="loadingRate"
                :readonly="isViewMode"
                required
                density="comfortable"
                prepend-icon="mdi-circle-small"
                class="invisible-icon"
                @input="rateSource = 'manual'"
              >
                <template v-slot:append-inner>
                  <v-btn
                    icon="mdi-refresh"
                    size="x-small"
                    variant="text"
                    @click="refreshRate"
                    :disabled="loadingRate || isViewMode"
                  ></v-btn>
                </template>
              </v-text-field>
            </v-col>
            <v-col cols="8">
              <v-text-field
                :model-value="convertedAmount"
                label="換算後金額"
                prepend-icon="mdi-circle-small"
                class="invisible-icon"
                readonly
                density="comfortable"
              ></v-text-field>
            </v-col>
          </v-row>

          <!-- 誰先收款 -->
          <v-card variant="outlined" class="mb-4 mt-4">
            <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
              <v-icon left size="small">mdi-account-multiple</v-icon>
              誰先收款
              <v-spacer></v-spacer>
              <v-chip size="small" color="primary">
                已收：{{ totalReceived }} / {{ formData.amount || 0 }}
              </v-chip>
            </v-card-title>
            <v-card-text>
              <v-list density="compact">
                <v-list-item v-for="member in members" :key="member.id">
                  <template v-slot:prepend>
                    <v-checkbox
                      v-model="selectedReceivers"
                      :value="member.id"
                      hide-details
                      density="compact"
                      :disabled="isViewMode"
                    ></v-checkbox>
                  </template>
                  <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                  <template v-slot:append>
                    <v-text-field
                      v-if="selectedReceivers.includes(member.id)"
                      v-model="receiveAmounts[member.id]"
                      type="number"
                      density="compact"
                      hide-details
                      style="width: 100px"
                      :readonly="isViewMode"
                    ></v-text-field>
                  </template>
                </v-list-item>
              </v-list>
              <v-btn
                v-if="selectedReceivers.length > 0 && !isViewMode"
                @click="autoDistributeReceived"
                size="small"
                variant="text"
                color="primary"
                class="mt-2"
              >
                平均分配已收金額
              </v-btn>
            </v-card-text>
          </v-card>

          <!-- 如何分攤 -->
          <v-card variant="outlined" class="mb-4">
            <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
              <v-icon left size="small">mdi-scale-unbalanced</v-icon>
              如何分攤
              <v-spacer></v-spacer>
              <v-chip size="small" color="primary">
                已分：{{ totalSplit }} / {{ formData.amount || 0 }}
              </v-chip>
            </v-card-title>
            <v-card-text>
              <v-radio-group
                v-model="splitType"
                hide-details
                class="mb-2"
                :disabled="isViewMode"
              >
                <v-radio label="平均分攤" value="equal"></v-radio>
                <v-radio label="自訂金額" value="custom"></v-radio>
              </v-radio-group>

              <v-list density="compact">
                <v-list-item v-for="member in members" :key="member.id">
                  <template v-slot:prepend>
                    <v-checkbox
                      v-model="selectedSplitters"
                      :value="member.id"
                      hide-details
                      density="compact"
                      :disabled="isViewMode"
                    ></v-checkbox>
                  </template>
                  <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                  <template v-slot:append>
                    <v-text-field
                      v-if="selectedSplitters.includes(member.id) && splitType === 'custom'"
                      v-model="splitAmounts[member.id]"
                      type="number"
                      density="compact"
                      hide-details
                      style="width: 100px"
                      :readonly="isViewMode"
                    ></v-text-field>
                    <v-chip
                      v-else-if="selectedSplitters.includes(member.id) && splitType === 'equal'"
                      size="small"
                    >
                      {{ equalSplitAmount }}
                    </v-chip>
                  </template>
                </v-list-item>
              </v-list>
            </v-card-text>
          </v-card>

          <!-- 備註 -->
          <v-textarea
            v-model="formData.notes"
            label="備註"
            prepend-icon="mdi-note-text"
            rows="2"
            auto-grow
            :readonly="isViewMode"
            density="comfortable"
          ></v-textarea>
        </v-form>

        <!-- 錯誤訊息 -->
        <v-alert v-if="errorMessage" type="error" class="mt-4">
          {{ errorMessage }}
        </v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <template v-if="mode === 'view'">
          <v-btn color="red" variant="elevated" @click="confirmDelete">
            <v-icon start>mdi-delete</v-icon>
            刪除
          </v-btn>
          <v-btn color="primary" variant="elevated" @click="startEditing">
            <v-icon start>mdi-pencil</v-icon>
            編輯
          </v-btn>
        </template>
        <template v-else-if="mode === 'edit'">
          <v-btn variant="text" @click="cancelEditing">取消</v-btn>
          <v-btn color="primary" variant="elevated" @click="saveChanges" :loading="loading">
            <v-icon start>mdi-content-save</v-icon>
            儲存
          </v-btn>
        </template>
        <template v-else>
          <v-btn color="grey" variant="text" @click="closeDialog">取消</v-btn>
          <v-btn color="green" variant="elevated" @click="submitIncome" :loading="loading">
            新增收入
          </v-btn>
        </template>
      </v-card-actions>
    </v-card>

    <!-- 刪除確認對話框 -->
    <v-dialog v-model="showDeleteConfirm" max-width="400px">
      <v-card>
        <v-card-title class="text-h6">確認刪除</v-card-title>
        <v-card-text>
          確定要刪除這筆收入嗎？此操作無法復原。
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="showDeleteConfirm = false">取消</v-btn>
          <v-btn color="red" variant="elevated" @click="deleteIncome" :loading="loading">
            確認刪除
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import api from '@/api/axios';
import { getDefaultRate } from '@/utils/defaultRates';

const props = defineProps({
  modelValue: Boolean,
  tripId: Number,
  members: Array,
  baseCurrency: String,
  transaction: {
    type: Object,
    default: null,
  },
});

const emit = defineEmits(['update:modelValue', 'refresh']);

const dialog = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

// 模式: 'add', 'view', 'edit'
const mode = ref('add');
const isViewMode = computed(() => mode.value === 'view');

const title = computed(() => {
  if (mode.value === 'add') return '新增收入';
  if (mode.value === 'edit') return '編輯收入';
  return '收入詳情';
});

const form = ref(null);
const valid = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const showDeleteConfirm = ref(false);
const rateSource = ref(''); // 'default' | 'group' | 'manual'
const loadingRate = ref(false);

const categories = ['退稅退費', '保險理賠', '贊助', '公積金', '意外之財'];

const incomePlaceholders = {
  '退稅退費': '退稅、退款、取消行程退費',
  '保險理賠': '班機延誤理賠、行李損毀、海外就醫理賠',
  '贊助': '長輩親友贊助基金',
  '公積金': '剩餘公積金分配退回',
  '意外之財': '買當地彩券中獎、活動抽中現金',
};

// 收入類別圖示映射
const incomeCategoryIcons = {
  '退稅退費': 'mdi-cash-refund',
  '保險理賠': 'mdi-shield-check-outline',
  '贊助': 'mdi-hand-heart',
  '公積金': 'mdi-piggy-bank-outline',
  '意外之財': 'mdi-bank-plus',
};

// 計算屬性：將 categories 轉換為 v-select 需要的帶圖示項目
const categorySelectItems = computed(() => {
  return categories.map(category => ({
    title: category,
    value: category,
    props: {
      prependIcon: incomeCategoryIcons[category] || 'mdi-help-circle-outline' // 預設圖示
    }
  }));
});

// 貨幣選項：包含預設貨幣和群組基礎貨幣（如果不在預設列表中）
const currencies = computed(() => {
  const defaultCurrencies = ['TWD', 'JPY', 'USD', 'EUR', 'CNY', 'GBP', 'KRW', 'THB', 'SGD', 'HKD', 'AUD'];
  if (props.baseCurrency && !defaultCurrencies.includes(props.baseCurrency)) {
    return [props.baseCurrency, ...defaultCurrencies];
  }
  return defaultCurrencies;
});

const formData = ref({
  incomeDate: new Date().toISOString().split('T')[0],
  title: '',
  category: '退稅退費',
  amount: '',
  currency: props.baseCurrency || 'TWD',
  exchangeRate: '',
  notes: '',
});

// 計算屬性：根據類別返回對應的 placeholder
const currentPlaceholder = computed(() => {
  return incomePlaceholders[formData.value.category] || '例如：景點退費、保險理賠';
});

const selectedReceivers = ref([]);
const receiveAmounts = ref({});
const selectedSplitters = ref([]);
const splitAmounts = ref({});
const splitType = ref('equal');

const rules = {
  required: (v) => !!v || '此欄位必填',
  positive: (v) => v > 0 || '金額必須大於 0',
};

const showExchangeRate = computed(() => {
  return (
    props.baseCurrency && formData.value.currency && props.baseCurrency !== formData.value.currency
  );
});

const convertedAmount = computed(() => {
  if (formData.value.amount && formData.value.exchangeRate) {
    const result = parseFloat(formData.value.amount) * parseFloat(formData.value.exchangeRate);
    return result.toFixed(2);
  }
  return '';
});

// 計算已收總額
const totalReceived = computed(() => {
  return selectedReceivers.value.reduce((sum, id) => {
    return sum + (parseFloat(receiveAmounts.value[id]) || 0);
  }, 0);
});

// 計算已分攤總額
const totalSplit = computed(() => {
  if (splitType.value === 'equal') {
    return selectedSplitters.value.length * parseFloat(equalSplitAmount.value);
  } else {
    return selectedSplitters.value.reduce((sum, id) => {
      return sum + (parseFloat(splitAmounts.value[id]) || 0);
    }, 0);
  }
});

// 平均分攤金額
const equalSplitAmount = computed(() => {
  if (!formData.value.amount || selectedSplitters.value.length === 0) return 0;
  return (parseFloat(formData.value.amount) / selectedSplitters.value.length).toFixed(2);
});

// 抬頭框顏色：收入類型的顏色
const headerColor = computed(() => '#4874299b');

// 自動分配已收金額
const autoDistributeReceived = () => {
  if (!formData.value.amount || selectedReceivers.value.length === 0) return;
  const perPerson = parseFloat(formData.value.amount) / selectedReceivers.value.length;
  selectedReceivers.value.forEach((id) => {
    receiveAmounts.value[id] = perPerson.toFixed(2);
  });
};

// 載入匯率（依序：群組紀錄 > 預設匯率 > 無）
const loadExchangeRate = async (from, to) => {
  // 參數驗證：確保 from 和 to 都是有效的字符串
  if (!from || !to || typeof from !== 'string' || typeof to !== 'string') {
    console.warn('loadExchangeRate: 無效的幣別參數', { from, to });
    formData.value.exchangeRate = '';
    rateSource.value = '';
    return;
  }

  if (from === to) {
    formData.value.exchangeRate = '1';
    rateSource.value = 'default';
    return;
  }

  // 確保 tripId 存在
  if (!props.tripId) {
    const defaultRate = getDefaultRate(from, to);
    if (defaultRate) {
      formData.value.exchangeRate = defaultRate.toString();
      rateSource.value = 'default';
    } else {
      formData.value.exchangeRate = '';
      rateSource.value = '';
    }
    return;
  }

  loadingRate.value = true;
  rateSource.value = '';

  try {
    // 1. 優先查詢群組紀錄
    const response = await api.get(`/trips/${props.tripId}/rates`, {
      params: { from, to },
    });

    if (response.data.rate) {
      formData.value.exchangeRate = response.data.rate.toString();
      rateSource.value = 'group';
      loadingRate.value = false;
      return;
    }
  } catch (error) {
    // 只記錄非 401 錯誤，避免觸發自動登出
    if (error.response?.status !== 401) {
      console.warn('查詢群組匯率失敗，使用預設匯率:', error.message);
    }
  }

  // 2. 其次使用預設匯率
  const defaultRate = getDefaultRate(from, to);
  if (defaultRate) {
    formData.value.exchangeRate = defaultRate.toString();
    rateSource.value = 'default';
  } else {
    formData.value.exchangeRate = '';
    rateSource.value = '';
  }

  loadingRate.value = false;
};

// 手動重新整理匯率
const refreshRate = () => {
  if (formData.value.currency && props.baseCurrency) {
    loadExchangeRate(formData.value.currency, props.baseCurrency);
  }
};

const handleCurrencyChange = async (newCurrency) => {
  if (newCurrency === props.baseCurrency) {
    formData.value.exchangeRate = '1';
    rateSource.value = 'default';
  } else {
    await loadExchangeRate(newCurrency, props.baseCurrency);
  }
};

// 監聽金額變化，自動更新分攤
watch(
  () => formData.value.amount,
  () => {
    if (splitType.value === 'equal' && selectedSplitters.value.length > 0) {
      // 平均分攤會自動更新
    }
  }
);



const initForm = (transaction) => {
  if (!transaction) return;

  formData.value = {
    incomeDate: transaction.transactionDate,
    title: transaction.title,
    category: transaction.category,
    amount: transaction.amount,
    currency: transaction.currency,
    exchangeRate: transaction.exchangeRate || '',
    notes: transaction.notes || '',
  };

  // 初始化收款人
  selectedReceivers.value = transaction.payments ? transaction.payments.map((p) => p.memberId) : [];
  receiveAmounts.value = {};
  if (transaction.payments) {
    transaction.payments.forEach((p) => {
      receiveAmounts.value[p.memberId] = p.amount;
    });
  }

  // 初始化分攤人
  selectedSplitters.value = transaction.splits ? transaction.splits.map((s) => s.memberId) : [];
  splitAmounts.value = {};
  if (transaction.splits) {
    transaction.splits.forEach((s) => {
      splitAmounts.value[s.memberId] = s.amount;
    });
  }

  // 判斷分攤類型
  const firstSplit = transaction.splits && transaction.splits.length > 0 ? transaction.splits[0] : null;
  splitType.value = firstSplit?.splitType === 'CUSTOM_AMOUNT' ? 'custom' : 'equal';
};

const validateForm = async () => {
  if (!form.value) return false;
  const { valid: isValid } = await form.value.validate();
  if (!isValid) {
    errorMessage.value = '請填寫所有必填欄位';
    return false;
  }

  if (selectedReceivers.value.length === 0) {
    errorMessage.value = '請選擇至少一位收款人';
    return false;
  }

  if (Math.abs(totalReceived.value - parseFloat(formData.value.amount)) > 0.01) {
    errorMessage.value = `收款金額總和（${totalReceived.value}）必須等於收入金額（${formData.value.amount}）`;
    return false;
  }

  if (selectedSplitters.value.length === 0) {
    errorMessage.value = '請選擇至少一位分攤者';
    return false;
  }

  if (Math.abs(totalSplit.value - parseFloat(formData.value.amount)) > 0.01) {
    errorMessage.value = `分攤金額總和（${totalSplit.value}）必須等於收入金額（${formData.value.amount}）`;
    return false;
  }

  return true;
};

const getPayload = () => {
  const receivers = selectedReceivers.value.map((id) => ({
    memberId: id,
    amount: parseFloat(receiveAmounts.value[id]),
  }));

  const splits = selectedSplitters.value.map((id) => ({
    memberId: id,
    amount:
      splitType.value === 'equal'
        ? parseFloat(equalSplitAmount.value)
        : parseFloat(splitAmounts.value[id]),
    splitType: splitType.value === 'equal' ? 'EQUAL' : 'CUSTOM_AMOUNT',
  }));

  let rate = null;
  if (formData.value.exchangeRate) {
    rate = parseFloat(formData.value.exchangeRate);
  } else if (formData.value.currency === props.baseCurrency) {
    rate = 1.0;
  }

  return {
    ...formData.value,
    amount: parseFloat(formData.value.amount),
    exchangeRate: rate,
    receivers,
    splits,
  };
};

const submitIncome = async () => {
  if (!(await validateForm())) return;

  loading.value = true;
  errorMessage.value = '';

  try {
    await api.post(`/trips/${props.tripId}/incomes`, getPayload());
    emit('refresh');
    closeDialog();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '新增失敗，請稍後再試';
  } finally {
    loading.value = false;
  }
};

const startEditing = () => {
  mode.value = 'edit';
};

const cancelEditing = () => {
  initForm(props.transaction);
  mode.value = 'view';
  errorMessage.value = '';
};

const saveChanges = async () => {
  if (!(await validateForm())) return;

  loading.value = true;
  errorMessage.value = '';

  try {
    const transactionId = props.transaction.transactionId.toString().replace('I', '');
    await api.put(`/trips/${props.tripId}/incomes/${transactionId}`, getPayload());
    emit('refresh');
    closeDialog();

  } catch (error) {
    errorMessage.value = error.response?.data?.message || '更新失敗，請稍後再試';
  } finally {
    loading.value = false;
  }
};

const confirmDelete = () => {
  showDeleteConfirm.value = true;
};

const deleteIncome = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const transactionId = props.transaction.transactionId.toString().replace('I', '');
    await api.delete(`/trips/${props.tripId}/incomes/${transactionId}`);
    showDeleteConfirm.value = false;
    emit('refresh');
    closeDialog();

  } catch (error) {
    errorMessage.value = error.response?.data?.message || '刪除失敗，請稍後再試';
    showDeleteConfirm.value = false;
  } finally {
    loading.value = false;
  }
};

const closeDialog = () => {
  dialog.value = false;
  resetForm();
};

const resetForm = () => {
  if (form.value) {
    form.value.reset();
  }
  formData.value = {
    incomeDate: new Date().toISOString().split('T')[0],
    title: '',
    category: '退稅退費',
    amount: '',
    currency: props.baseCurrency || 'TWD',
    exchangeRate: '',
    notes: '',
  };
  selectedReceivers.value = [];
  receiveAmounts.value = {};
  selectedSplitters.value = [];
  splitAmounts.value = {};
  splitType.value = 'equal';
  errorMessage.value = '';
};

// 監聽 props.transaction 變化，初始化表單
watch(
  () => props.transaction,
  (newVal) => {
    if (newVal) {
      mode.value = 'view';
      initForm(newVal);
    } else {
      mode.value = 'add';
    }
  },
  { immediate: true }
);

// 監聽 dialog 開啟，初始化表單
watch(dialog, (val) => {
  if (val) {
    if (props.transaction) {
      // 查看模式：重新初始化表單數據
      mode.value = 'view';
      initForm(props.transaction);
    } else {
      // 新增模式：重置表單
      mode.value = 'add';
      resetForm();
    }
  }
});
</script>

<style scoped>
/* 固定標題樣式 */
.modal-card {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
}

.scrollable-content {
  overflow-y: auto;
  max-height: calc(90vh - 64px - 52px); /* 90vh - header - footer */
}

.invisible-icon :deep(.v-input__prepend) {
  opacity: 0;
}

/* 隱藏數字輸入框的上下箭頭 */
:deep(input[type="number"]::-webkit-outer-spin-button),
:deep(input[type="number"]::-webkit-inner-spin-button) {
  -webkit-appearance: none;
  margin: 0;
}

:deep(input[type="number"]) {
  -moz-appearance: textfield;
}
</style>
