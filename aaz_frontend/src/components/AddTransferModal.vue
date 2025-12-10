<template>
  <v-dialog v-model="dialog" max-width="500px" persistent>
    <v-card>
      <v-card-title class="text-h5 bg-blue-lighten-4">
        <v-icon left color="blue">mdi-swap-horizontal</v-icon>
        新增轉帳
      </v-card-title>

      <v-card-text class="pt-6">
        <v-form ref="form" v-model="valid">
          <!-- 日期 -->
          <v-text-field
            v-model="formData.transferDate"
            label="日期"
            type="date"
            prepend-icon="mdi-calendar"
            :rules="[rules.required]"
            required
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

          <!-- 備註 -->
          <v-textarea
            v-model="formData.notes"
            label="備註（選填）"
            prepend-icon="mdi-note-text"
            rows="2"
            auto-grow
            placeholder="例如：還午餐錢、飯店房費"
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
        <v-btn color="grey" variant="text" @click="closeDialog">取消</v-btn>
        <v-btn color="blue" variant="elevated" @click="submitTransfer" :loading="loading">
          新增轉帳
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup>
import { ref, computed } from 'vue';
import api from '@/api/axios';

const props = defineProps({
  modelValue: Boolean,
  tripId: Number,
  members: Array,
  baseCurrency: String,
});

const emit = defineEmits(['update:modelValue', 'transfer-added']);

const dialog = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const form = ref(null);
const valid = ref(false);
const loading = ref(false);
const errorMessage = ref('');

const currencies = ['TWD', 'JPY', 'USD', 'EUR', 'CNY', 'KRW', 'THB', 'SGD', 'HKD'];

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

// 提交轉帳
const submitTransfer = async () => {
  if (!form.value) return;

  const { valid: isValid } = await form.value.validate();
  if (!isValid) {
    errorMessage.value = '請填寫所有必填欄位';
    return;
  }

  // 再次驗證不能轉給自己
  if (formData.value.fromMemberId === formData.value.toMemberId) {
    errorMessage.value = '轉帳人和收款人不能是同一個人';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const payload = {
      ...formData.value,
      amount: parseFloat(formData.value.amount),
      exchangeRate: formData.value.exchangeRate ? parseFloat(formData.value.exchangeRate) : null,
    };

    await api.post(`/trips/${props.tripId}/transfers`, payload);
    emit('transfer-added');
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
</script>

<style scoped>
.text-blue {
  color: #2196f3;
}
</style>
