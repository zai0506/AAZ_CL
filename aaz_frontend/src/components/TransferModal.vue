<template>
  <v-dialog v-model="dialog" max-width="600px" persistent>
    <v-card>
      <v-card-title class="d-flex align-center justify-space-between bg-primary text-white">
        <span class="text-h6">{{ title }}</span>
        <v-btn icon="mdi-close" variant="text" size="small" color="white" @click="closeDialog"></v-btn>
      </v-card-title>

      <v-card-text class="pt-6">
        <v-form ref="form" v-model="valid" :disabled="isViewMode">
          <!-- 日期 -->
          <v-text-field
            v-model="formData.transferDate"
            label="日期"
            type="date"
            prepend-icon="mdi-calendar"
            :rules="[rules.required]"
            required
            density="comfortable"
          ></v-text-field>

          <!-- 誰轉帳 -->
          <v-select
            v-model="formData.fromMemberId"
            :items="memberOptions"
            item-title="displayName"
            item-value="id"
            label="誰轉帳"
            prepend-icon="mdi-account-arrow-right"
            :rules="[rules.required]"
            required
            density="comfortable"
          >
            <template v-slot:item="{ props, item }">
              <v-list-item v-bind="props" :title="item.raw.displayName">
                <template v-slot:prepend>
                  <v-icon color="red">mdi-account-minus</v-icon>
                </template>
              </v-list-item>
            </template>
          </v-select>

          <!-- 轉帳給誰 -->
          <v-select
            v-model="formData.toMemberId"
            :items="receiverOptions"
            item-title="displayName"
            item-value="id"
            label="轉帳給誰"
            prepend-icon="mdi-account-arrow-left"
            :rules="[rules.required, rules.notSamePerson]"
            required
            density="comfortable"
          >
            <template v-slot:item="{ props, item }">
              <v-list-item v-bind="props" :title="item.raw.displayName">
                <template v-slot:prepend>
                  <v-icon color="green">mdi-account-plus</v-icon>
                </template>
              </v-list-item>
            </template>
          </v-select>

          <!-- 提示：不能轉給自己 -->
          <v-alert
            v-if="formData.fromMemberId && formData.fromMemberId === formData.toMemberId"
            type="warning"
            density="compact"
            class="mb-4"
          >
            轉帳人和收款人不能是同一個人
          </v-alert>

          <!-- 貨幣和金額 -->
          <v-row>
            <v-col cols="4">
              <v-select
                v-model="formData.currency"
                :items="currencies"
                label="貨幣"
                :rules="[rules.required]"
                @update:modelValue="handleCurrencyChange"
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
                prepend-icon="mdi-currency-usd"
                :rules="[rules.required, rules.positive]"
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
                prepend-icon="mdi-currency-usd"
                readonly
                density="comfortable"
              ></v-text-field>
            </v-col>
          </v-row>

          <!-- 備註 -->
          <v-textarea
            v-model="formData.notes"
            label="備註"
            prepend-icon="mdi-note-text"
            rows="2"
            auto-grow
            density="comfortable"
          ></v-textarea>

          <!-- 轉帳示意圖 -->
          <v-card
            v-if="formData.fromMemberId && formData.toMemberId && formData.amount"
            variant="outlined"
            class="mt-4 pa-4"
          >
            <div class="d-flex align-center justify-center">
              <v-chip color="red" class="mr-4">
                <v-icon left>mdi-account</v-icon>
                {{ getDisplayName(formData.fromMemberId) }}
              </v-chip>

              <div class="text-center">
                <v-icon size="large" color="blue">mdi-arrow-right-bold</v-icon>
                <div class="text-h6 text-blue">{{ formData.currency }} {{ formData.amount }}</div>
              </div>

              <v-chip color="green" class="ml-4">
                <v-icon left>mdi-account</v-icon>
                {{ getDisplayName(formData.toMemberId) }}
              </v-chip>
            </div>
          </v-card>
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
          <v-btn color="blue" variant="elevated" @click="submitTransfer" :loading="loading">
            新增轉帳
          </v-btn>
        </template>
      </v-card-actions>
    </v-card>

    <!-- 刪除確認對話框 -->
    <v-dialog v-model="showDeleteConfirm" max-width="400px">
      <v-card>
        <v-card-title class="text-h6">確認刪除</v-card-title>
        <v-card-text>
          確定要刪除這筆轉帳嗎？此操作無法復原。
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="showDeleteConfirm = false">取消</v-btn>
          <v-btn color="red" variant="elevated" @click="deleteTransfer" :loading="loading">
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
  if (mode.value === 'add') return '新增轉帳';
  if (mode.value === 'edit') return '編輯轉帳';
  return '轉帳詳情';
});

const form = ref(null);
const valid = ref(false);
const loading = ref(false);
const errorMessage = ref('');
const showDeleteConfirm = ref(false);
const rateSource = ref(''); // 'default' | 'group' | 'manual'
const loadingRate = ref(false);

// 貨幣選項：包含預設貨幣和群組基礎貨幣（如果不在預設列表中）
const currencies = computed(() => {
  const defaultCurrencies = ['TWD', 'JPY', 'USD', 'EUR', 'CNY', 'GBP', 'KRW', 'THB', 'SGD', 'HKD', 'AUD'];
  if (props.baseCurrency && !defaultCurrencies.includes(props.baseCurrency)) {
    return [props.baseCurrency, ...defaultCurrencies];
  }
  return defaultCurrencies;
});

const formData = ref({
  transferDate: new Date().toISOString().split('T')[0],
  fromMemberId: null,
  toMemberId: null,
  amount: '',
  currency: props.baseCurrency || 'TWD',
  exchangeRate: '',
  notes: '',
});

const memberOptions = computed(() => props.members || []);

const receiverOptions = computed(() => {
  // 過濾掉轉帳人，避免轉給自己
  return props.members?.filter((m) => m.id !== formData.value.fromMemberId) || [];
});

const rules = {
  required: (v) => !!v || '此欄位必填',
  positive: (v) => v > 0 || '金額必須大於 0',
  notSamePerson: (v) => {
    if (!v || !formData.value.fromMemberId) return true;
    return v !== formData.value.fromMemberId || '轉帳人和收款人不能是同一個人';
  },
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

const getDisplayName = (memberId) => {
  const member = props.members?.find((m) => m.id === memberId);
  return member?.displayName || '';
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

const initForm = (transaction) => {
  if (!transaction) return;

  // 轉帳的 payments[0] 是付款人 (from)，splits[0] 是收款人 (to)
  // 這是根據後端 convertTransferToResponse 的邏輯
  const fromId = transaction.payments?.[0]?.memberId;
  const toId = transaction.splits?.[0]?.memberId;

  formData.value = {
    transferDate: transaction.transactionDate,
    fromMemberId: fromId,
    toMemberId: toId,
    amount: transaction.amount,
    currency: transaction.currency,
    exchangeRate: transaction.exchangeRate || '',
    notes: transaction.notes || '',
  };
};

const validateForm = async () => {
  if (!form.value) return false;
  const { valid: isValid } = await form.value.validate();
  if (!isValid) {
    errorMessage.value = '請填寫所有必填欄位';
    return false;
  }

  if (formData.value.fromMemberId === formData.value.toMemberId) {
    errorMessage.value = '轉帳人和收款人不能是同一個人';
    return false;
  }

  return true;
};

const getPayload = () => {
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
  };
};

const submitTransfer = async () => {
  if (!(await validateForm())) return;

  loading.value = true;
  errorMessage.value = '';

  try {
    await api.post(`/trips/${props.tripId}/transfers`, getPayload());
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
    const transactionId = props.transaction.transactionId.toString().replace('T', '');
    await api.put(`/trips/${props.tripId}/transfers/${transactionId}`, getPayload());
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

const deleteTransfer = async () => {
  loading.value = true;
  errorMessage.value = '';

  try {
    const transactionId = props.transaction.transactionId.toString().replace('T', '');
    await api.delete(`/trips/${props.tripId}/transfers/${transactionId}`);
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
    transferDate: new Date().toISOString().split('T')[0],
    fromMemberId: null,
    toMemberId: null,
    amount: '',
    currency: props.baseCurrency || 'TWD',
    exchangeRate: '',
    notes: '',
  };
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

// 監聽 dialog 開啟，如果是新增模式，重置表單
watch(dialog, (val) => {
  if (val && !props.transaction) {
    mode.value = 'add';
    resetForm();
  }
});
</script>

<style scoped>
.text-blue {
  color: #2196f3;
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
