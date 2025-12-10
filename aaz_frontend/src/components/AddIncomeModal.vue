<template>
  <v-dialog v-model="dialog" max-width="600px" persistent>
    <v-card>
      <v-card-title class="text-h5 bg-green-lighten-4">
        <v-icon left color="green">mdi-cash-plus</v-icon>
        新增收入
      </v-card-title>

      <v-card-text class="pt-6">
        <v-form ref="form" v-model="valid">
          <!-- 日期 -->
          <v-text-field
            v-model="formData.incomeDate"
            label="日期"
            type="date"
            prepend-icon="mdi-calendar"
            :rules="[rules.required]"
            required
          ></v-text-field>

          <!-- 品項 -->
          <v-text-field
            v-model="formData.title"
            label="品項"
            prepend-icon="mdi-format-title"
            :rules="[rules.required]"
            placeholder="例如：景點退費、保險理賠"
            required
          ></v-text-field>

          <!-- 類別 -->
          <v-select
            v-model="formData.category"
            :items="categories"
            label="類別"
            prepend-icon="mdi-shape"
            :rules="[rules.required]"
            required
          ></v-select>

          <!-- 貨幣和金額 -->
          <v-row>
            <v-col cols="4">
              <v-select
                v-model="formData.currency"
                :items="currencies"
                label="貨幣"
                :rules="[rules.required]"
                @update:modelValue="formData.exchangeRate = ''"
                required
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
                :hint="`1 ${formData.currency} / ${baseCurrency}`"
                :rules="[rules.required, rules.positive]"
                required
              ></v-text-field>
            </v-col>
            <v-col cols="8">
              <v-text-field
                :model-value="convertedAmount"
                label="換算後金額"
                prepend-icon="mdi-currency-usd"
                readonly
              ></v-text-field>
            </v-col>
          </v-row>

          <!-- 誰先收款 -->
          <v-card variant="outlined" class="mb-4">
            <v-card-title class="text-subtitle-1 bg-grey-lighten-4">
              <v-icon left size="small">mdi-wallet</v-icon>
              誰先收款
              <v-spacer></v-spacer>
              <v-chip size="small" color="primary">
                已收：{{ totalReceived }} / {{ formData.amount || 0 }}
              </v-chip>
            </v-card-title>
            <v-card-text>
              <v-list>
                <v-list-item v-for="member in members" :key="member.id">
                  <template v-slot:prepend>
                    <v-checkbox
                      v-model="selectedReceivers"
                      :value="member.id"
                      hide-details
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
                    ></v-text-field>
                  </template>
                </v-list-item>
              </v-list>
              <v-btn
                v-if="selectedReceivers.length > 0"
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
              <v-icon left size="small">mdi-account-multiple</v-icon>
              如何分攤
              <v-spacer></v-spacer>
              <v-chip size="small" color="primary">
                已分：{{ totalSplit }} / {{ formData.amount || 0 }}
              </v-chip>
            </v-card-title>
            <v-card-text>
              <v-radio-group v-model="splitType" hide-details class="mb-2">
                <v-radio label="平均分攤" value="equal"></v-radio>
                <v-radio label="自訂金額" value="custom"></v-radio>
              </v-radio-group>

              <v-list>
                <v-list-item v-for="member in members" :key="member.id">
                  <template v-slot:prepend>
                    <v-checkbox
                      v-model="selectedSplitters"
                      :value="member.id"
                      hide-details
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
            label="備註（選填）"
            prepend-icon="mdi-note-text"
            rows="2"
            auto-grow
          ></v-textarea>
        </v-form>

        <!-- 錯誤訊息 -->
        <v-alert v-if="errorMessage" type="error" class="mt-4">
          {{ errorMessage }}
        </v-alert>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="grey" variant="text" @click="closeDialog">取消</v-btn>
        <v-btn color="green" variant="elevated" @click="submitIncome" :loading="loading">
          新增收入
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import api from '@/api/axios';

const props = defineProps({
  modelValue: Boolean,
  tripId: Number,
  members: Array,
  baseCurrency: String,
});

const emit = defineEmits(['update:modelValue', 'income-added']);

const dialog = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const form = ref(null);
const valid = ref(false);
const loading = ref(false);
const errorMessage = ref('');

const categories = ['退費', '保險', '禮金', '獎金', '其他'];
const currencies = ['TWD', 'JPY', 'USD', 'EUR', 'CNY', 'KRW', 'THB', 'SGD', 'HKD'];

const formData = ref({
  incomeDate: new Date().toISOString().split('T')[0],
  title: '',
  category: '退費',
  amount: '',
  currency: props.baseCurrency || 'TWD',
  exchangeRate: '',
  notes: '',
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

// 自動分配已收金額
const autoDistributeReceived = () => {
  if (!formData.value.amount || selectedReceivers.value.length === 0) return;
  const perPerson = parseFloat(formData.value.amount) / selectedReceivers.value.length;
  selectedReceivers.value.forEach((id) => {
    receiveAmounts.value[id] = perPerson.toFixed(2);
  });
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

// 提交收入
const submitIncome = async () => {
  if (!form.value) return;

  const { valid: isValid } = await form.value.validate();
  if (!isValid) {
    errorMessage.value = '請填寫所有必填欄位';
    return;
  }

  // 驗證收款人
  if (selectedReceivers.value.length === 0) {
    errorMessage.value = '請選擇至少一位收款人';
    return;
  }

  if (Math.abs(totalReceived.value - parseFloat(formData.value.amount)) > 0.01) {
    errorMessage.value = `收款金額總和（${totalReceived.value}）必須等於收入金額（${formData.value.amount}）`;
    return;
  }

  // 驗證分攤
  if (selectedSplitters.value.length === 0) {
    errorMessage.value = '請選擇至少一位分攤者';
    return;
  }

  if (Math.abs(totalSplit.value - parseFloat(formData.value.amount)) > 0.01) {
    errorMessage.value = `分攤金額總和（${totalSplit.value}）必須等於收入金額（${formData.value.amount}）`;
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
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

    const payload = {
      ...formData.value,
      amount: parseFloat(formData.value.amount),
      exchangeRate: formData.value.exchangeRate ? parseFloat(formData.value.exchangeRate) : null,
      receivers,
      splits,
    };

    await api.post(`/trips/${props.tripId}/incomes`, payload);
    emit('income-added');
    closeDialog();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '新增失敗，請稍後再試';
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
    category: '退費',
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
</script>
