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
            v-model="formData.expenseDate"
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

          <!-- 誰先付錢 -->
          <v-card variant="outlined" class="mb-4 mt-4">
            <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
              <v-icon left size="small">mdi-account-multiple</v-icon>
              誰先付錢
              <v-spacer></v-spacer>
              <v-chip size="small" color="primary">
                已付：{{ totalPaid }} / {{ formData.amount || 0 }}
              </v-chip>
            </v-card-title>
            <v-card-text>
              <v-list density="compact">
                <v-list-item v-for="member in members" :key="member.id">
                  <template v-slot:prepend>
                    <v-checkbox
                      v-model="selectedPayers"
                      :value="member.id"
                      hide-details
                      density="compact"
                      :disabled="isViewMode"
                    ></v-checkbox>
                  </template>
                  <v-list-item-title>{{ member.displayName }}</v-list-item-title>
                  <template v-slot:append>
                    <v-text-field
                      v-if="selectedPayers.includes(member.id)"
                      v-model="paymentAmounts[member.id]"
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
                v-if="selectedPayers.length > 0 && !isViewMode"
                @click="autoDistributePaid"
                size="small"
                variant="text"
                color="primary"
                class="mt-2"
              >
                平均分配已付金額
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
          <v-btn color="red" variant="elevated" @click="submitExpense" :loading="loading">
            新增支出
          </v-btn>
        </template>
      </v-card-actions>
    </v-card>

    <!-- 刪除確認對話框 -->
    <v-dialog v-model="showDeleteConfirm" max-width="400px">
      <v-card>
        <v-card-title class="text-h6">確認刪除</v-card-title>
        <v-card-text>
          確定要刪除這筆支出嗎？此操作無法復原。
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="showDeleteConfirm = false">取消</v-btn>
          <v-btn color="red" variant="elevated" @click="deleteExpense" :loading="loading">
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
// const isEditMode = computed(() => mode.value === 'edit');

const title = computed(() => {
  if (mode.value === 'add') return '新增支出';
  if (mode.value === 'edit') return '編輯支出';
  return '支出詳情';
});

const form = ref(null);
const valid = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const showDeleteConfirm = ref(false);
const rateSource = ref(''); // 'default' | 'group' | 'manual'
const loadingRate = ref(false);

const categories = ['美食', '服飾', '住宿', '藥妝日用', '交通', '景點活動','禮品', '零碎支出'];

const expensePlaceholders = {
  '美食': '餐廳、街邊小吃',
  '服飾': '衣服、褲子、鞋子',
  '住宿': '飯店、民宿',
  '藥妝日用': '藥妝、防曬、日用品',
  '交通': '機票、租車、大眾運輸',
  '景點活動': '門票、潛水、手作體驗、滑雪',
  '禮品': '伴手禮',
  '零碎支出': 'sim卡、漫游、夾娃娃',
};

// 支出類別圖示映射
const expenseCategoryIcons = {
  '美食': 'mdi-silverware-fork-knife',
  '服飾': 'mdi-tshirt-crew-outline',
  '住宿': 'mdi-bed-outline',
  '藥妝日用': 'mdi-cart-variant',
  '交通': 'mdi-plane-train',
  '景點活動': 'mdi-ski',
  '禮品': 'mdi-gift-open-outline',
  '零碎支出': 'mdi-cat',
};

// 計算屬性：將 categories 轉換為 v-select 需要的帶圖示項目
const categorySelectItems = computed(() => {
  return categories.map(category => ({
    title: category,
    value: category,
    props: {
      prependIcon: expenseCategoryIcons[category] || 'mdi-help-circle-outline' // 預設圖示
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
  expenseDate: new Date().toISOString().split('T')[0],
  title: '',
  category: '美食',
  amount: '',
  currency: props.baseCurrency || 'TWD',
  exchangeRate: '',
  notes: '',
});

// 計算屬性：根據類別返回對應的 placeholder
const currentPlaceholder = computed(() => {
  return expensePlaceholders[formData.value.category] || '例如：午餐、飯店住宿';
});

const selectedPayers = ref([]);
const paymentAmounts = ref({});
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

// 計算已付總額
const totalPaid = computed(() => {
  return selectedPayers.value.reduce((sum, id) => {
    return sum + (parseFloat(paymentAmounts.value[id]) || 0);
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

// 抬頭框顏色：支出類型的顏色
const headerColor = computed(() => '#E67E66');

// 自動分配已付金額
const autoDistributePaid = () => {
  if (!formData.value.amount || selectedPayers.value.length === 0) return;
  const perPerson = parseFloat(formData.value.amount) / selectedPayers.value.length;
  selectedPayers.value.forEach((id) => {
    paymentAmounts.value[id] = perPerson.toFixed(2);
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
    expenseDate: transaction.transactionDate,
    title: transaction.title,
    category: transaction.category,
    amount: transaction.amount,
    currency: transaction.currency,
    exchangeRate: transaction.exchangeRate || '',
    notes: transaction.notes || '',
  };

  // 初始化付款人
  selectedPayers.value = transaction.payments ? transaction.payments.map((p) => p.memberId) : [];
  paymentAmounts.value = {};
  if (transaction.payments) {
    transaction.payments.forEach((p) => {
      paymentAmounts.value[p.memberId] = p.amount;
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

  if (selectedPayers.value.length === 0) {
    errorMessage.value = '請選擇至少一位付款人';
    return false;
  }

  if (Math.abs(totalPaid.value - parseFloat(formData.value.amount)) > 0.01) {
    errorMessage.value = `付款金額總和（${totalPaid.value}）必須等於支出金額（${formData.value.amount}）`;
    return false;
  }

  if (selectedSplitters.value.length === 0) {
    errorMessage.value = '請選擇至少一位分攤者';
    return false;
  }

  if (Math.abs(totalSplit.value - parseFloat(formData.value.amount)) > 0.01) {
    errorMessage.value = `分攤金額總和（${totalSplit.value}）必須等於支出金額（${formData.value.amount}）`;
    return false;
  }

  return true;
};

const getPayload = () => {
  const payments = selectedPayers.value.map((id) => ({
    memberId: id,
    amount: parseFloat(paymentAmounts.value[id]),
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
    payments,
    splits,
  };
};

const submitExpense = async () => {
  if (!(await validateForm())) return;

  loading.value = true;
  errorMessage.value = '';

  try {
    await api.post(`/trips/${props.tripId}/expenses`, getPayload());
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
  // 恢復原始資料
  initForm(props.transaction);
  mode.value = 'view';
  errorMessage.value = '';
};

const saveChanges = async () => {
  if (!(await validateForm())) return;

  loading.value = true;
  errorMessage.value = '';

  try {
    // transactionId 可能有前綴 'E'，需要去掉
    const transactionId = props.transaction.transactionId.toString().replace('E', '');
    await api.put(`/trips/${props.tripId}/expenses/${transactionId}`, getPayload());
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

const deleteExpense = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const transactionId = props.transaction.transactionId.toString().replace('E', '');
    await api.delete(`/trips/${props.tripId}/expenses/${transactionId}`);
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
    expenseDate: new Date().toISOString().split('T')[0],
    title: '',
    category: '美食',
    amount: '',
    currency: props.baseCurrency || 'TWD',
    exchangeRate: '',
    notes: '',
  };
  selectedPayers.value = [];
  paymentAmounts.value = {};
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
